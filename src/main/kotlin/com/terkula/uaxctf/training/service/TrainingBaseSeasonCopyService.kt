package com.terkula.uaxctf.training.service

import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.training.model.trainingbase.TrainingBasePerformance
import com.terkula.uaxctf.training.repository.TrainingBasePerformanceRepository
import com.terkula.uaxctf.training.request.CopyBaseTrainingPacesRequest
import com.terkula.uaxctf.training.response.CopyBaseTrainingPaceResult
import com.terkula.uaxctf.training.response.CopyBaseTrainingPacesResponse
import com.terkula.uaxctf.util.toMinuteSecondString
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Carries base training paces from one season into another (e.g. XC 5k -> track 5000m),
 * with an optional slow-down adjustment for fitness lost during the post-season break.
 */
@Service
class TrainingBaseSeasonCopyService(
        val trainingBasePerformanceRepository: TrainingBasePerformanceRepository,
        val runnerRepository: RunnerRepository
) {

    companion object {
        const val SOURCE_SEASON_COPY = "SEASON_COPY"
        const val ADJ_NONE = "NONE"
        const val ADJ_PERCENT = "PERCENT"
        const val ADJ_SECONDS_PER_MILE = "SECONDS_PER_MILE"
        const val ADJ_TOTAL_SECONDS = "TOTAL_SECONDS"
        const val MAX_TOTAL_SECONDS = 300.0
        const val MAX_PERCENT = 25.0
        const val MAX_SECONDS_PER_MILE = 120.0
    }

    @Transactional
    fun copyBaseTrainingPaces(request: CopyBaseTrainingPacesRequest): CopyBaseTrainingPacesResponse {
        validate(request)

        val sourceByRunner = latestPerRunner(
                trainingBasePerformanceRepository.findByEventAndSeasonAndYear(request.fromEvent, request.fromSeason, request.fromYear))
        val targetByRunner = latestPerRunner(
                trainingBasePerformanceRepository.findByEventAndSeasonAndYear(request.toEvent, request.toSeason, request.toYear))

        val runnerIds = if (request.runnerIds.isEmpty()) sourceByRunner.keys.toList() else request.runnerIds.distinct()
        val runners = runnerRepository.findAllById(runnerIds).associateBy { it.id }

        val detail = sourceDetail(request)

        val results = runnerIds.mapNotNull { runnerId ->
            val runner = runners[runnerId] ?: return@mapNotNull null
            val source = sourceByRunner[runnerId]
            val existing = targetByRunner[runnerId]

            if (source == null) {
                return@mapNotNull CopyBaseTrainingPaceResult(runner, "NO_SOURCE", null, null, null, null, null,
                        existing?.seconds, existing?.pacePerMile(), existing?.source)
            }

            val newSeconds = adjustedSeconds(source, request)
            val status = when {
                existing == null -> if (request.dryRun) "WILL_CREATE" else "CREATED"
                request.overwriteExisting -> if (request.dryRun) "WILL_REPLACE" else "REPLACED"
                else -> "SKIPPED_EXISTING"
            }

            if (!request.dryRun && status != "SKIPPED_EXISTING") {
                trainingBasePerformanceRepository.save(TrainingBasePerformance(
                        runnerId = runnerId,
                        fractionOfMiles = source.fractionOfMiles,
                        seconds = newSeconds,
                        season = request.toSeason,
                        event = request.toEvent,
                        year = request.toYear,
                        uuid = UUID.randomUUID().toString().uppercase(),
                        source = SOURCE_SEASON_COPY,
                        derivedFromBasePerformanceId = source.id,
                        sourceDetail = "$detail (was ${source.seconds.toDouble().toMinuteSecondString()})"
                ))
            }

            CopyBaseTrainingPaceResult(
                    runner = runner,
                    status = status,
                    fractionOfMiles = source.fractionOfMiles,
                    sourceSeconds = source.seconds,
                    sourcePacePerMile = source.pacePerMile(),
                    newSeconds = newSeconds,
                    newPacePerMile = (newSeconds / source.fractionOfMiles).toMinuteSecondString(),
                    existingSeconds = existing?.seconds,
                    existingPacePerMile = existing?.pacePerMile(),
                    existingSource = existing?.source
            )
        }.sortedWith(compareBy(nullsLast()) { it.newSeconds })

        val created = results.count { it.status == "CREATED" || it.status == "REPLACED" }
        val skipped = results.count { it.status == "SKIPPED_EXISTING" || it.status == "NO_SOURCE" }

        return CopyBaseTrainingPacesResponse(
                dryRun = request.dryRun,
                fromSeason = request.fromSeason,
                fromYear = request.fromYear,
                fromEvent = request.fromEvent,
                toSeason = request.toSeason,
                toYear = request.toYear,
                toEvent = request.toEvent,
                adjustmentType = request.adjustmentType.uppercase(),
                adjustmentValue = request.adjustmentValue,
                createdCount = created,
                skippedCount = skipped,
                results = results
        )
    }

    fun adjustedSeconds(source: TrainingBasePerformance, request: CopyBaseTrainingPacesRequest): Int {
        val seconds = source.seconds.toDouble()
        val adjusted = when (request.adjustmentType.uppercase()) {
            ADJ_PERCENT -> seconds * (1 + request.adjustmentValue / 100.0)
            ADJ_SECONDS_PER_MILE -> seconds + request.adjustmentValue * source.fractionOfMiles
            ADJ_TOTAL_SECONDS -> seconds + request.adjustmentValue
            else -> seconds
        }
        return adjusted.roundToInt()
    }

    private fun validate(request: CopyBaseTrainingPacesRequest) {
        val type = request.adjustmentType.uppercase()
        if (type !in listOf(ADJ_NONE, ADJ_PERCENT, ADJ_SECONDS_PER_MILE, ADJ_TOTAL_SECONDS)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "adjustmentType must be NONE, PERCENT, SECONDS_PER_MILE or TOTAL_SECONDS")
        }
        if (type == ADJ_PERCENT && abs(request.adjustmentValue) > MAX_PERCENT) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "percent adjustment must be within ±$MAX_PERCENT")
        }
        if (type == ADJ_SECONDS_PER_MILE && abs(request.adjustmentValue) > MAX_SECONDS_PER_MILE) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "seconds per mile adjustment must be within ±$MAX_SECONDS_PER_MILE")
        }
        if (type == ADJ_TOTAL_SECONDS && abs(request.adjustmentValue) > MAX_TOTAL_SECONDS) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "total seconds adjustment must be within ±$MAX_TOTAL_SECONDS")
        }
        if (!request.dryRun && request.runnerIds.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "runnerIds is required when dryRun is false")
        }
        if (request.fromSeason == request.toSeason && request.fromYear == request.toYear && request.fromEvent == request.toEvent) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "source and target are the same")
        }
    }

    private fun sourceDetail(request: CopyBaseTrainingPacesRequest): String {
        val adjustment = when (request.adjustmentType.uppercase()) {
            ADJ_PERCENT -> "${signed(request.adjustmentValue)}%"
            ADJ_SECONDS_PER_MILE -> "${signed(request.adjustmentValue)}s/mi"
            ADJ_TOTAL_SECONDS -> "${signed(request.adjustmentValue)}s total"
            else -> "no adjustment"
        }
        return "Copied from ${request.fromSeason} ${request.fromYear} ${request.fromEvent}, $adjustment"
    }

    private fun signed(value: Double): String {
        val text = if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
        return if (value >= 0) "+$text" else text
    }

    private fun TrainingBasePerformance.pacePerMile(): String = (seconds / fractionOfMiles).toMinuteSecondString()

    // Newest record per runner wins, matching how the rest of the app reads base paces.
    private fun latestPerRunner(records: List<TrainingBasePerformance>): Map<Int, TrainingBasePerformance> =
            records.groupBy { it.runnerId }
                    .mapValues { (_, perfs) -> perfs.maxByOrNull { it.createdAt?.time ?: Long.MIN_VALUE }!! }
}
