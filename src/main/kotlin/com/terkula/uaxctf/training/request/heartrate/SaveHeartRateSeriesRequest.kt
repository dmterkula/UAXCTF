package com.terkula.uaxctf.training.request.heartrate

class HeartRatePointRequest(
    val offsetSeconds: Int,
    val bpm: Int
)

class PacePointRequest(
    val offsetSeconds: Int,
    val secPerMile: Int
)

class SaveHeartRateSeriesRequest(
    val activityType: String,
    val activityUuid: String,
    val runnerId: Int,
    val durationSeconds: Int?,
    val source: String?,
    val samples: List<HeartRatePointRequest>,
    // Optional: absent when the source has no distance data (older app versions, some Garmin syncs, indoor runs).
    val paceSamples: List<PacePointRequest>? = null
)
