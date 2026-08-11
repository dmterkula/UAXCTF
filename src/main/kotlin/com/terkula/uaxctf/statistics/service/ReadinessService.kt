package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statistics.response.ReadinessSignal
import com.terkula.uaxctf.statistics.response.RunnerReadinessResponse
import com.terkula.uaxctf.statistics.response.TeamReadinessResponse
import com.terkula.uaxctf.statistics.response.WeeklyAttendance
import com.terkula.uaxctf.training.response.getTotalDistance
import com.terkula.uaxctf.training.response.getTotalSeconds
import com.terkula.uaxctf.training.response.trainingdashboard.RunnerTrainingDashboardEntry
import com.terkula.uaxctf.training.service.TrainingDashboardService
import com.terkula.uaxctf.util.calculateSecondsFrom
import org.springframework.stereotype.Service
import java.sql.Date
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// Hardcoded defaults — used only if a DB row is missing (should never happen after migration)
private object ReadinessDefaults {
    const val WORKOUT_PROXY_EFFORT = 7.5
    const val CT_DEFAULT_EFFORT = 5.0
    const val CT_LOAD_MULTIPLIER = 0.8
    const val EWMA_LAMBDA = 0.3
    const val RECOVERY_CEILING = 5.0
    const val DEFAULT_CEILING = 7.0
    const val PAIN_YELLOW = 3.0
    const val PAIN_RED = 5.0
    const val SPIKE_YELLOW_PCT = 15.0
    const val SPIKE_RED_PCT = 30.0
    const val MIN_WEEKLY_LOGS = 3.0
    const val WINDOW_WEEKS = 5.0
}

/** All config values fetched once per request and passed into computeReadiness. */
private data class ReadinessConfig(
    val workoutProxyEffort: Double,
    val ctDefaultEffort: Double,
    val ctLoadMultiplier: Double,
    val ewmaLambda: Double,
    val recoveryCeiling: Double,
    val defaultCeiling: Double,
    val painYellow: Double,
    val painRed: Double,
    val spikeYellowPct: Double,
    val spikeRedPct: Double,
    val minWeeklyLogs: Int,
    val windowWeeks: Int
)

@Service
class ReadinessService(
    val trainingDashboardService: TrainingDashboardService,
    val configService: TrainingLoadConfigService
) {

    fun getTeamReadiness(season: String, referenceDate: Date, team: String?): TeamReadinessResponse {
        val cfg = loadConfig(team)
        val refLocal = referenceDate.toLocalDate()
        val windowStart = Date.valueOf(refLocal.minusDays((cfg.windowWeeks * 7).toLong()))

        val dashboard = trainingDashboardService.getTeamTrainingDashboard(season, windowStart, referenceDate, team)

        val readiness = dashboard.runnerTrainingDashboardEntries
            .map { entry: RunnerTrainingDashboardEntry -> computeReadiness(entry, refLocal, cfg) }
            .sortedByDescending { r: RunnerReadinessResponse -> statusOrder(r.status) }

        return TeamReadinessResponse(readiness)
    }

    private fun loadConfig(team: String?) = ReadinessConfig(
        workoutProxyEffort = configService.get("readiness.workout_proxy_effort",   team, ReadinessDefaults.WORKOUT_PROXY_EFFORT),
        ctDefaultEffort    = configService.get("readiness.ct_default_effort",       team, ReadinessDefaults.CT_DEFAULT_EFFORT),
        ctLoadMultiplier   = configService.get("readiness.cross_training_load_multiplier", team, ReadinessDefaults.CT_LOAD_MULTIPLIER),
        ewmaLambda         = configService.get("readiness.ewma_lambda",             team, ReadinessDefaults.EWMA_LAMBDA),
        recoveryCeiling    = configService.get("readiness.recovery_effort_ceiling", team, ReadinessDefaults.RECOVERY_CEILING),
        defaultCeiling     = configService.get("readiness.default_effort_ceiling",  team, ReadinessDefaults.DEFAULT_CEILING),
        painYellow         = configService.get("readiness.pain_yellow_threshold",   team, ReadinessDefaults.PAIN_YELLOW),
        painRed            = configService.get("readiness.pain_red_threshold",      team, ReadinessDefaults.PAIN_RED),
        spikeYellowPct     = configService.get("readiness.spike_yellow_pct",        team, ReadinessDefaults.SPIKE_YELLOW_PCT),
        spikeRedPct        = configService.get("readiness.spike_red_pct",           team, ReadinessDefaults.SPIKE_RED_PCT),
        minWeeklyLogs      = configService.get("readiness.min_weekly_logs",         team, ReadinessDefaults.MIN_WEEKLY_LOGS).toInt(),
        windowWeeks        = configService.get("readiness.window_weeks",            team, ReadinessDefaults.WINDOW_WEEKS).toInt()
    )

    private fun statusOrder(status: String) = when (status) {
        "RED" -> 2; "YELLOW" -> 1; else -> 0
    }

    private data class ActivityEntry(val date: LocalDate, val load: Double)

    private fun computeReadiness(
        entry: RunnerTrainingDashboardEntry,
        refDate: LocalDate,
        cfg: ReadinessConfig
    ): RunnerReadinessResponse {
        val signals = mutableListOf<ReadinessSignal>()
        val activities = mutableListOf<ActivityEntry>()

        // Training runs: distance × effort (impact-bearing — no multiplier)
        for (runResult in entry.trainingRuns) {
            val runDate = runResult.trainingRun.date.toLocalDate()
            val dto = runResult.results.firstOrNull() ?: continue
            val effort = dto.effortLevel ?: continue
            activities.add(ActivityEntry(runDate, dto.getTotalDistance() * effort))
        }

        // Workouts: distance × effort (impact-bearing — no multiplier)
        for (workout in entry.workouts) {
            val wDate = workout.workout.date.toLocalDate()
            val effort = workout.effortLevel ?: cfg.workoutProxyEffort
            activities.add(ActivityEntry(wDate, workout.getTotalDistance() * effort))
        }

        // Cross-training: time(min) × effort × multiplier
        // Time-based (Foster sRPE) to be modality-agnostic; multiplier adjusts for lower
        // musculoskeletal stress vs running (1.0 = pure aerobic, 0.8 = aerobic+musculoskeletal)
        for (ct in entry.crossTrainingRecords) {
            val ctDate = ct.crossTrainingEvent.date.toLocalDate()
            val effort = ct.crossTrainingRecord.effortLevel ?: cfg.ctDefaultEffort
            val durationMinutes = ct.crossTrainingRecord.time.calculateSecondsFrom() / 60.0
            activities.add(ActivityEntry(ctDate, durationMinutes * effort * cfg.ctLoadMultiplier))
        }

        // Weekly buckets: [0] = trailing 7 days (most recent), [windowWeeks-1] = oldest
        fun weekStart(i: Int): LocalDate = refDate.minusDays(((i + 1) * 7).toLong())
        fun weekEnd(i: Int): LocalDate   = refDate.minusDays((i * 7).toLong())
        fun inWeek(date: LocalDate, i: Int) = date.isAfter(weekStart(i)) && !date.isAfter(weekEnd(i))

        val weeklyLoads  = (0 until cfg.windowWeeks).map { i -> activities.filter { inWeek(it.date, i) }.sumOf { it.load } }
        val weeklyCounts = (0 until cfg.windowWeeks).map { i -> activities.count { inWeek(it.date, i) } }

        val week0Load  = weeklyLoads[0];  val week1Load  = weeklyLoads[1]
        val week0Count = weeklyCounts[0]; val week1Count = weeklyCounts[1]

        val dataComplete = !(week0Count < (week1Count * 0.6).toInt() && week0Count < cfg.minWeeklyLogs)

        // --- Signal 1: Logging frequency ---
        when {
            week0Count == 0 ->
                signals.add(ReadinessSignal("LOGGING_GAP", "RED",
                    "No activities logged in the last 7 days",
                    sortValue = cfg.minWeeklyLogs.toDouble()))
            week0Count < cfg.minWeeklyLogs ->
                signals.add(ReadinessSignal("LOGGING_GAP", "YELLOW",
                    "Only $week0Count activities logged this week (expected ≥${cfg.minWeeklyLogs})",
                    sortValue = (cfg.minWeeklyLogs - week0Count).toDouble()))
        }

        // --- Signal 2: Pain ---
        val sevenDaysAgo = refDate.minusDays(7)
        val recentPainRuns = entry.trainingRuns
            .filter { it.trainingRun.date.toLocalDate().isAfter(sevenDaysAgo) }
            .flatMap { it.results }
            .filter { (it.painLevel ?: 0.0) > cfg.painYellow }

        if (recentPainRuns.isNotEmpty()) {
            val hasRed = recentPainRuns.any { (it.painLevel ?: 0.0) >= cfg.painRed || !it.painNotes.isNullOrBlank() }
            val maxPain = recentPainRuns.mapNotNull { it.painLevel }.maxOrNull() ?: cfg.painYellow
            signals.add(ReadinessSignal("PAIN", if (hasRed) "RED" else "YELLOW",
                "Pain level $maxPain reported in the last 7 days",
                sortValue = maxPain))
        }

        // --- Signal 3: Effort vs day-type ceiling ---
        entry.trainingRuns
            .filter { it.trainingRun.date.toLocalDate().isAfter(sevenDaysAgo) }
            .forEach { runResult ->
                val label = runResult.trainingRun.effortLabel?.uppercase() ?: "N/A"
                val ceiling = if (label == "RECOVERY") cfg.recoveryCeiling else cfg.defaultCeiling
                val dto = runResult.results.firstOrNull() ?: return@forEach
                val effort = dto.effortLevel ?: return@forEach
                val overage = effort - ceiling
                if (overage > 2) {
                    signals.add(ReadinessSignal("EFFORT_HIGH", "RED",
                        "Effort $effort on $label day (ceiling $ceiling) — ${runResult.trainingRun.date}",
                        sortValue = overage))
                } else if (overage > 0) {
                    signals.add(ReadinessSignal("EFFORT_HIGH", "YELLOW",
                        "Effort $effort on $label day (ceiling $ceiling) — ${runResult.trainingRun.date}",
                        sortValue = overage))
                }
            }

        // --- Signal 4: Load spike ---
        val wowPct: Double? = if (week1Load > 0) ((week0Load - week1Load) / week1Load * 100.0) else null
        if (wowPct != null && wowPct > 0 && dataComplete) {
            when {
                wowPct > cfg.spikeRedPct ->
                    signals.add(ReadinessSignal("LOAD_SPIKE", "RED",
                        "Load up ${wowPct.toInt()}% week-over-week (${"%.1f".format(week1Load)} → ${"%.1f".format(week0Load)} sRPE)",
                        sortValue = wowPct))
                wowPct > cfg.spikeYellowPct ->
                    signals.add(ReadinessSignal("LOAD_SPIKE", "YELLOW",
                        "Load up ${wowPct.toInt()}% week-over-week (${"%.1f".format(week1Load)} → ${"%.1f".format(week0Load)} sRPE)",
                        sortValue = wowPct))
            }
        }

        val ewmaLoad = computeEwma(weeklyLoads.reversed(), cfg.ewmaLambda)
        val lastDate = activities.maxByOrNull { it.date }?.date
        val daysSinceLastLog = lastDate?.let { ChronoUnit.DAYS.between(it, refDate).toInt() } ?: 999

        val status = when {
            signals.any { it.severity == "RED" } -> "RED"
            signals.any { it.severity == "YELLOW" } -> "YELLOW"
            else -> "GREEN"
        }

        val attendance = computeWeeklyAttendance(entry, refDate)

        return RunnerReadinessResponse(
            runner = entry.runner,
            status = status,
            signals = signals,
            weeklyLoad = weeklyLoads[0],
            loadWowPct = wowPct,
            ewmaLoad = ewmaLoad,
            loggedSessions7d = week0Count,
            daysSinceLastLog = daysSinceLastLog,
            dataComplete = dataComplete,
            weeklyLoads = weeklyLoads,
            attendance = attendance
        )
    }

    // Current-week logging attendance: which elapsed Mon–Sat days the runner has no log for.
    // A day "counts" if the runner logged any training run, workout, cross-training, or meet on it.
    private fun computeWeeklyAttendance(
        entry: RunnerTrainingDashboardEntry,
        refDate: LocalDate
    ): WeeklyAttendance {
        val monday = refDate.minusDays((refDate.dayOfWeek.value - 1).toLong())
        // Expected = Mon–Sat of this week, but only days that have already elapsed (strictly before today).
        val expected = (0L..5L)
            .map { monday.plusDays(it) }
            .filter { it.isBefore(refDate) }

        val loggedDates = mutableSetOf<LocalDate>()
        entry.trainingRuns
            .filter { it.results.isNotEmpty() }
            .forEach { loggedDates.add(it.trainingRun.date.toLocalDate()) }
        entry.workouts.forEach { loggedDates.add(it.workout.date.toLocalDate()) }
        entry.crossTrainingRecords.forEach { loggedDates.add(it.crossTrainingEvent.date.toLocalDate()) }
        entry.xcMeetLogs.forEach { it.meet?.let { m -> loggedDates.add(m.date.toLocalDate()) } }
        entry.trackMeetLogs.forEach { it.trackMeet?.let { tm -> loggedDates.add(tm.date.toLocalDate()) } }

        val logged = expected.filter { it in loggedDates }
        val missing = expected.filter { it !in loggedDates }

        return WeeklyAttendance(
            weekStart = monday.toString(),
            expectedDays = expected.map { it.toString() },
            loggedDays = logged.map { it.toString() },
            missingDays = missing.map { it.toString() },
            missingCount = missing.size
        )
    }

    private fun computeEwma(loadsOldestFirst: List<Double>, lambda: Double): Double {
        if (loadsOldestFirst.isEmpty()) return 0.0
        var ewma = loadsOldestFirst[0]
        for (i in 1 until loadsOldestFirst.size) {
            ewma = lambda * loadsOldestFirst[i] + (1 - lambda) * ewma
        }
        return ewma
    }
}
