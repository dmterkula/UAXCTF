package com.terkula.uaxctf.training.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.terkula.uaxctf.training.model.heartrate.HeartRateSeries
import com.terkula.uaxctf.training.repository.HeartRateSeriesRepository
import com.terkula.uaxctf.training.request.heartrate.SaveHeartRateSeriesRequest
import com.terkula.uaxctf.training.response.heartrate.HeartRatePointDTO
import com.terkula.uaxctf.training.response.heartrate.PacePointDTO
import com.terkula.uaxctf.training.response.heartrate.HeartRateSeriesResponse
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException

@Component
class HeartRateSeriesService(
    val heartRateSeriesRepository: HeartRateSeriesRepository,
    // Spring's mapper has the Kotlin module registered; a bare ObjectMapper() can't build Kotlin DTOs.
    private val mapper: ObjectMapper
) {

    companion object {
        val ACTIVITY_TYPES = setOf("training_run", "workout", "cross_training")
        const val MAX_SAMPLES = 2000
        const val MIN_VALID_BPM = 30
        const val MAX_VALID_BPM = 250
        // 3:00/mi .. 30:00/mi; anything outside is GPS/sensor noise, not running.
        const val MIN_VALID_PACE = 180
        const val MAX_VALID_PACE = 1800
    }

    fun get(activityType: String, activityUuid: String, runnerId: Int): HeartRateSeriesResponse? {
        val row = heartRateSeriesRepository
            .findByActivityTypeAndActivityUuidAndRunnerId(activityType, activityUuid, runnerId)
            .firstOrNull() ?: return null
        return toResponse(row)
    }

    fun save(request: SaveHeartRateSeriesRequest): HeartRateSeriesResponse {
        if (request.activityType !in ACTIVITY_TYPES) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown activityType")
        }

        // Drop impossible values and out-of-order/duplicate offsets rather than rejecting the whole upload.
        val cleaned = request.samples
            .filter { it.offsetSeconds >= 0 && it.bpm in MIN_VALID_BPM..MAX_VALID_BPM }
            .sortedBy { it.offsetSeconds }
            .distinctBy { it.offsetSeconds }
            .map { HeartRatePointDTO(it.offsetSeconds, it.bpm) }

        val cleanedPace = (request.paceSamples ?: emptyList())
            .filter { it.offsetSeconds >= 0 && it.secPerMile in MIN_VALID_PACE..MAX_VALID_PACE }
            .sortedBy { it.offsetSeconds }
            .distinctBy { it.offsetSeconds }
            .map { PacePointDTO(it.offsetSeconds, it.secPerMile) }

        // Heart rate and pace are independent: a workout may have either, both, or (rejected) neither.
        val hasHr = cleaned.size >= 2
        val hasPace = cleanedPace.size >= 2
        if (!hasHr && !hasPace) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Need at least 2 valid heart rate or pace samples")
        }
        if (cleaned.size > MAX_SAMPLES || cleanedPace.size > MAX_SAMPLES) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Too many samples (max $MAX_SAMPLES)")
        }
        val hrPoints = if (hasHr) cleaned else emptyList()

        val duration = request.durationSeconds ?: maxOf(hrPoints.lastOrNull()?.offsetSeconds ?: 0, cleanedPace.lastOrNull()?.offsetSeconds ?: 0)
        val bpms = hrPoints.map { it.bpm }
        val json = mapper.writeValueAsString(hrPoints)
        val paceJson = if (hasPace) mapper.writeValueAsString(cleanedPace) else null

        val existing = heartRateSeriesRepository
            .findByActivityTypeAndActivityUuidAndRunnerId(request.activityType, request.activityUuid, request.runnerId)
            .firstOrNull()

        val row = existing ?: HeartRateSeries(
            request.activityType, request.activityUuid, request.runnerId,
            hrPoints.size, duration, null, null, null, json, request.source
        )
        row.sampleCount = hrPoints.size
        row.durationSeconds = duration
        row.avgHr = if (bpms.isEmpty()) null else bpms.average().toInt()
        row.maxHr = bpms.maxOrNull()
        row.minHr = bpms.minOrNull()
        row.samples = json
        row.paceSamples = paceJson
        row.source = request.source
        heartRateSeriesRepository.save(row)

        return toResponse(row)
    }

    fun delete(activityType: String, activityUuid: String, runnerId: Int) {
        heartRateSeriesRepository.deleteByActivityTypeAndActivityUuidAndRunnerId(activityType, activityUuid, runnerId)
    }

    private fun toResponse(row: HeartRateSeries): HeartRateSeriesResponse {
        val points: List<HeartRatePointDTO> = mapper.readValue(row.samples, object : TypeReference<List<HeartRatePointDTO>>() {})
        return HeartRateSeriesResponse(
            row.activityType, row.activityUuid, row.runnerId, row.durationSeconds,
            row.avgHr, row.maxHr, row.minHr, row.source, points,
            row.paceSamples?.let { mapper.readValue(it, object : TypeReference<List<PacePointDTO>>() {}) }
        )
    }
}
