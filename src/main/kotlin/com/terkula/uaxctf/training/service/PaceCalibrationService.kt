package com.terkula.uaxctf.training.service

import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.statistics.service.TrainingLoadConfigService
import com.terkula.uaxctf.training.model.trainingbase.TrainingBasePerformance
import com.terkula.uaxctf.training.repository.WorkoutComponentRepository
import com.terkula.uaxctf.training.repository.WorkoutSplitV2Repository
import com.terkula.uaxctf.training.repository.WorkoutRepository
import com.terkula.uaxctf.training.response.calibration.*
import com.terkula.uaxctf.util.calculateSecondsFrom
import com.terkula.uaxctf.util.secondsPerMileToPercentPacePerMile
import com.terkula.uaxctf.util.toMinuteSecondString
import org.springframework.stereotype.Service
import java.sql.Date
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min

// Zone classification constants
private const val ZONE_CLASS_WORKOUT = "WORKOUT"
private const val ZONE_CLASS_RUN = "RUN"

// Configurable defaults
object CalibrationDefaults {
    const val TARGET_SAMPLES_WORKOUT = 3.0
    const val TARGET_SAMPLES_RUN = 6.0
    const val MIN_SAMPLES_WORKOUT = 2.0
    const val MIN_SAMPLES_RUN = 4.0
    const val SAMPLE_WEIGHT_WORKOUT = 2.0
    const val SAMPLE_WEIGHT_RUN = 1.0
    const val UNIFORM_BAND = 0.015       // 1.5% — all zones within this → BASE_OFF
    const val ZONE_OUTLIER_BAND = 0.025  // 2.5% — one zone outside this vs others → ZONE_OFF
    const val MIN_SIGNAL = 0.015         // 1.5% — must exceed this to report any verdict
    const val STD_CEILING = 0.04         // 4% stdDev → consistency = 0
    const val RECENCY_HALF_LIFE_DAYS = 10.0
    const val MAX_PAIN_LEVEL = 4.0
}

@Service
class PaceCalibrationService(
    private val trainingRunsService: TrainingRunsService,
    private val trainingBasePerformanceService: TrainingBasePerformanceService,
    private val workoutRepository: WorkoutRepository,
    private val workoutComponentRepository: WorkoutComponentRepository,
    private val workoutSplitV2Repository: WorkoutSplitV2Repository,
    private val configService: TrainingLoadConfigService
) {

    fun calibrateRunner(
        runner: Runner,
        season: String,
        year: String,
        startDate: Date,
        endDate: Date,
        team: String? = null
    ): RunnerPaceCalibration {

        val cfg = CalibrationConfig(
            targetSamplesWorkout = configService.get("calibration.target_samples.workout", team, CalibrationDefaults.TARGET_SAMPLES_WORKOUT),
            targetSamplesRun     = configService.get("calibration.target_samples.run",     team, CalibrationDefaults.TARGET_SAMPLES_RUN),
            minSamplesWorkout    = configService.get("calibration.min_samples.workout",    team, CalibrationDefaults.MIN_SAMPLES_WORKOUT),
            minSamplesRun        = configService.get("calibration.min_samples.run",        team, CalibrationDefaults.MIN_SAMPLES_RUN),
            sampleWeightWorkout  = configService.get("calibration.sample_weight.workout",  team, CalibrationDefaults.SAMPLE_WEIGHT_WORKOUT),
            sampleWeightRun      = configService.get("calibration.sample_weight.run",      team, CalibrationDefaults.SAMPLE_WEIGHT_RUN),
            uniformBand          = configService.get("calibration.uniform_band",           team, CalibrationDefaults.UNIFORM_BAND),
            zoneOutlierBand      = configService.get("calibration.zone_outlier_band",      team, CalibrationDefaults.ZONE_OUTLIER_BAND),
            minSignal            = configService.get("calibration.min_signal",             team, CalibrationDefaults.MIN_SIGNAL),
            stdCeiling           = configService.get("calibration.std_ceiling",            team, CalibrationDefaults.STD_CEILING),
            recencyHalfLifeDays  = configService.get("calibration.recency_half_life_days", team, CalibrationDefaults.RECENCY_HALF_LIFE_DAYS),
            maxPainLevel         = configService.get("calibration.max_pain_level",         team, CalibrationDefaults.MAX_PAIN_LEVEL)
        )

        val basePerformance = trainingBasePerformanceService
            .getRunnersTrainingBasePerformance(runner.id, season, year)
            .trainingBasePerformance

        val trainingSamples = collectTrainingRunSamples(runner.id, season, startDate, endDate, basePerformance, cfg, endDate)
        val workoutSamples  = collectWorkoutSamples(runner.id, season, startDate, endDate, basePerformance, cfg, endDate)

        val allSamples = trainingSamples + workoutSamples
        if (allSamples.isEmpty()) {
            return insufficientData(runner, basePerformance)
        }

        val zones = buildZoneCalibrations(allSamples, cfg, endDate)
        val (confidence, confidenceLabel) = computeRunnerConfidence(zones, basePerformance, cfg)
        val (verdict, suggestion) = computeVerdict(zones, basePerformance, cfg)

        val baseSource = basePerformance?.source ?: "COACH_ESTIMATE"
        val currentBaseTime = basePerformance?.let {
            (it.seconds / it.fractionOfMiles).toMinuteSecondString()
        } ?: "N/A"

        return RunnerPaceCalibration(
            runner = runner,
            currentBaseTime = currentBaseTime,
            baseSource = baseSource,
            confidence = confidence,
            confidenceLabel = confidenceLabel,
            verdict = verdict,
            suggestion = suggestion,
            zones = zones
        )
    }

    // --- Training run samples ---

    private fun collectTrainingRunSamples(
        runnerId: Int,
        season: String,
        startDate: Date,
        endDate: Date,
        basePerformance: TrainingBasePerformance?,
        cfg: CalibrationConfig,
        referenceDate: Date
    ): List<PaceSample> {
        return try {
            trainingRunsService.getARunnersTrainingRunsByTypeWithinDates(runnerId, startDate, endDate, season)
                .trainingRunResults
                .flatMap { it.results }
                .filter { run ->
                    run.paceRange != null &&
                    (run.painLevel == null || run.painLevel < cfg.maxPainLevel)
                }
                .mapNotNull { run ->
                    val range = run.paceRange ?: return@mapNotNull null
                    val fastSec = range.fastEndPace.calculateSecondsFrom()
                    val slowSec = range.slowEndPace.calculateSecondsFrom()
                    if (fastSec <= 0 || slowSec <= 0) return@mapNotNull null

                    val expectedMid = (fastSec + slowSec) / 2.0
                    val actualSec = run.avgPace.calculateSecondsFrom()
                    if (actualSec <= 0) return@mapNotNull null

                    val relDev = (actualSec - expectedMid) / expectedMid
                    val zoneName = run.paceRange?.baseTrainingEvent ?: "recovery"

                    PaceSample(
                        zoneKey      = zoneName.lowercase(),
                        zoneClass    = ZONE_CLASS_RUN,
                        relDev       = relDev,
                        expectedPace = expectedMid.toMinuteSecondString(),
                        actualPace   = run.avgPace,
                        date         = referenceDate
                    )
                }
        } catch (_: Exception) { emptyList() }
    }

    // --- Workout samples ---

    private fun collectWorkoutSamples(
        runnerId: Int,
        season: String,
        startDate: Date,
        endDate: Date,
        basePerformance: TrainingBasePerformance?,
        cfg: CalibrationConfig,
        referenceDate: Date
    ): List<PaceSample> {
        if (basePerformance == null) return emptyList()

        val basePacePerMile = basePerformance.seconds / basePerformance.fractionOfMiles

        return try {
            workoutRepository.findByDateBetweenAndSeason(startDate, endDate, season)
                .flatMap { workout ->
                    workoutComponentRepository.findByWorkoutUuid(workout.uuid)
                        .filter { it.percent != null }
                        .mapNotNull { component ->
                            val splits = workoutSplitV2Repository
                                .findByComponentUUIDAndRunnerId(component.uuid, runnerId)
                            if (splits.isEmpty()) return@mapNotNull null

                            val targetPaceSec = basePacePerMile
                                .secondsPerMileToPercentPacePerMile(component.percent!!)
                                .calculateSecondsFrom()
                            if (targetPaceSec <= 0) return@mapNotNull null

                            val avgActualSec = splits.map { it.value.calculateSecondsFrom() }.average()
                            if (avgActualSec <= 0) return@mapNotNull null

                            val relDev = (avgActualSec - targetPaceSec) / targetPaceSec
                            val zoneKey = zoneKeyFromPercent(component.percent!!)

                            PaceSample(
                                zoneKey      = zoneKey,
                                zoneClass    = ZONE_CLASS_WORKOUT,
                                relDev       = relDev,
                                expectedPace = targetPaceSec.toMinuteSecondString(),
                                actualPace   = avgActualSec.toMinuteSecondString(),
                                date         = workout.date
                            )
                        }
                }
        } catch (_: Exception) { emptyList() }
    }

    // Map a percent to a human-readable zone name matching the spec
    private fun zoneKeyFromPercent(percent: Int): String = when {
        percent >= 113 -> "800 pace"
        percent >= 108 -> "mile pace"
        percent >= 103 -> "vo2"
        percent >= 98  -> "threshold+"
        percent >= 93  -> "threshold 90"
        percent >= 90  -> "sub threshold"
        percent >= 86  -> "tempo"
        percent >= 83  -> "base building+"
        percent >= 80  -> "base building"
        percent >= 78  -> "base building 80%"
        percent >= 73  -> "recovery+"
        else           -> "recovery"
    }

    // --- Zone aggregation ---

    private fun buildZoneCalibrations(
        samples: List<PaceSample>,
        cfg: CalibrationConfig,
        referenceDate: Date
    ): List<ZoneCalibration> {
        return samples.groupBy { it.zoneKey }.map { (zoneKey, zoneSamples) ->
            val zoneClass = zoneSamples.first().zoneClass
            val n = zoneSamples.size
            val meanRelDev = zoneSamples.map { it.relDev }.average()
            val stdDev = if (n > 1) {
                val variance = zoneSamples.map { (it.relDev - meanRelDev) * (it.relDev - meanRelDev) }.average()
                kotlin.math.sqrt(variance)
            } else 0.0

            val inCount   = zoneSamples.count { abs(it.relDev) <= cfg.minSignal }
            val fastCount = zoneSamples.count { it.relDev < -cfg.minSignal }
            val slowCount = zoneSamples.count { it.relDev > cfg.minSignal }

            val mostRecentDate = zoneSamples.maxOf { it.date }
            val zoneConfidence = computeZoneConfidence(n, stdDev, mostRecentDate, referenceDate, zoneClass, cfg)

            val minSamples = if (zoneClass == ZONE_CLASS_WORKOUT) cfg.minSamplesWorkout else cfg.minSamplesRun
            val sufficientData = n >= minSamples

            ZoneCalibration(
                paceType        = zoneKey,
                zoneClass       = zoneClass,
                n               = n,
                meanRelDev      = meanRelDev,
                expectedPace    = zoneSamples.first().expectedPace,
                observedAvgPace = zoneSamples.map { it.actualPace.calculateSecondsFrom() }.average().toMinuteSecondString(),
                inCount         = inCount,
                fastCount       = fastCount,
                slowCount       = slowCount,
                confidence      = zoneConfidence,
                sufficientData  = sufficientData
            )
        }
    }

    // --- Per-zone confidence ---

    private fun computeZoneConfidence(
        n: Int,
        stdDev: Double,
        mostRecentDate: Date,
        referenceDate: Date,
        zoneClass: String,
        cfg: CalibrationConfig
    ): Int {
        val targetSamples = if (zoneClass == ZONE_CLASS_WORKOUT) cfg.targetSamplesWorkout else cfg.targetSamplesRun
        val sampleScore      = min(n / targetSamples, 1.0)
        val consistencyScore = 1.0 - (stdDev / cfg.stdCeiling).coerceIn(0.0, 1.0)
        val daysSince        = daysBetween(mostRecentDate, referenceDate).toDouble()
        val recencyScore     = exp(-0.693 * daysSince / cfg.recencyHalfLifeDays) // half-life decay
        return (100 * (0.45 * sampleScore + 0.35 * consistencyScore + 0.20 * recencyScore)).toInt()
    }

    // --- Runner-level confidence (sample-weighted blend) ---

    private fun computeRunnerConfidence(
        zones: List<ZoneCalibration>,
        basePerformance: TrainingBasePerformance?,
        cfg: CalibrationConfig
    ): Pair<Int, String> {
        val sufficientZones = zones.filter { it.sufficientData }
        if (sufficientZones.isEmpty()) return Pair(0, "LOW")

        val totalWeight = sufficientZones.sumOf {
            it.n * if (it.zoneClass == ZONE_CLASS_WORKOUT) cfg.sampleWeightWorkout else cfg.sampleWeightRun
        }
        val weightedConfidence = sufficientZones.sumOf {
            val w = it.n * if (it.zoneClass == ZONE_CLASS_WORKOUT) cfg.sampleWeightWorkout else cfg.sampleWeightRun
            it.confidence * w
        } / totalWeight

        val raw = weightedConfidence.toInt()

        // Anchor gate: coach-estimated base caps at MEDIUM
        val isRaceAnchored = basePerformance?.source == "RACE_DERIVED"
        val capped = if (!isRaceAnchored && raw > 60) 60 else raw

        val label = when {
            capped >= 70 -> "HIGH"
            capped >= 40 -> "MEDIUM"
            else         -> "LOW"
        }
        return Pair(capped, label)
    }

    // --- Verdict + suggestion ---

    private fun computeVerdict(
        zones: List<ZoneCalibration>,
        basePerformance: TrainingBasePerformance?,
        cfg: CalibrationConfig
    ): Pair<String, CalibrationSuggestion?> {
        val sufficientZones = zones.filter { it.sufficientData }
        if (sufficientZones.isEmpty()) return Pair("INSUFFICIENT_DATA", null)

        val meanDevs = sufficientZones.map { it.meanRelDev }
        val overallMean = meanDevs.average()
        val spread = (meanDevs.maxOrNull()!! - meanDevs.minOrNull()!!)

        // All zones cluster together AND signal exceeds threshold → base is off
        if (spread <= cfg.uniformBand && abs(overallMean) > cfg.minSignal) {
            val suggestion = basePerformance?.let { base ->
                val currentSec = base.seconds.toDouble()
                val suggestedSec = (currentSec * (1 + overallMean)).toInt()
                val deltaSec = suggestedSec - currentSec.toInt()
                val sign = if (deltaSec > 0) "+" else ""
                CalibrationSuggestion(
                    knob           = "BASE",
                    currentValue   = (currentSec / base.fractionOfMiles).toMinuteSecondString(),
                    suggestedValue = (suggestedSec / base.fractionOfMiles).toMinuteSecondString(),
                    deltaHuman     = "${sign}${deltaSec}s on base",
                    applicable     = true
                )
            }
            return Pair("BASE_OFF", suggestion)
        }

        // One zone is an outlier vs the rest → that zone's % is off
        if (sufficientZones.size >= 2) {
            val outlier = sufficientZones.maxByOrNull { zone ->
                val othersAvg = sufficientZones.filter { it != zone }.map { it.meanRelDev }.average()
                abs(zone.meanRelDev - othersAvg)
            }
            if (outlier != null) {
                val othersAvg = sufficientZones.filter { it != outlier }.map { it.meanRelDev }.average()
                val outlierDiff = abs(outlier.meanRelDev - othersAvg)
                if (outlierDiff >= cfg.zoneOutlierBand) {
                    val applicable = outlier.zoneClass == ZONE_CLASS_RUN
                    val suggestion = CalibrationSuggestion(
                        knob           = "ZONE_PERCENT",
                        currentValue   = outlier.paceType,
                        suggestedValue = outlier.paceType,
                        deltaHuman     = buildZoneDeltaHuman(outlier),
                        applicable     = applicable
                    )
                    return Pair("ZONE_OFF", suggestion)
                }
            }
        }

        // Within signal threshold → dialed in
        if (abs(overallMean) <= cfg.minSignal) {
            return Pair("DIALED_IN", null)
        }

        return Pair("INSUFFICIENT_DATA", null)
    }

    private fun buildZoneDeltaHuman(zone: ZoneCalibration): String {
        val direction = if (zone.meanRelDev > 0) "slower" else "faster"
        val pct = "%.1f".format(abs(zone.meanRelDev) * 100)
        return "${zone.paceType} runs ~${pct}% $direction than target — consider adjusting this zone's %"
    }

    private fun insufficientData(runner: Runner, base: TrainingBasePerformance?): RunnerPaceCalibration {
        return RunnerPaceCalibration(
            runner          = runner,
            currentBaseTime = base?.let { (it.seconds / it.fractionOfMiles).toMinuteSecondString() } ?: "N/A",
            baseSource      = base?.source ?: "COACH_ESTIMATE",
            confidence      = 0,
            confidenceLabel = "LOW",
            verdict         = "INSUFFICIENT_DATA",
            suggestion      = null,
            zones           = emptyList()
        )
    }

    private fun daysBetween(from: Date, to: Date): Long {
        return (to.time - from.time) / (1000 * 60 * 60 * 24)
    }
}

// Internal data classes (not part of the API response)
data class PaceSample(
    val zoneKey: String,
    val zoneClass: String,
    val relDev: Double,
    val expectedPace: String,
    val actualPace: String,
    val date: Date
)

data class CalibrationConfig(
    val targetSamplesWorkout: Double,
    val targetSamplesRun: Double,
    val minSamplesWorkout: Double,
    val minSamplesRun: Double,
    val sampleWeightWorkout: Double,
    val sampleWeightRun: Double,
    val uniformBand: Double,
    val zoneOutlierBand: Double,
    val minSignal: Double,
    val stdCeiling: Double,
    val recencyHalfLifeDays: Double,
    val maxPainLevel: Double
)
