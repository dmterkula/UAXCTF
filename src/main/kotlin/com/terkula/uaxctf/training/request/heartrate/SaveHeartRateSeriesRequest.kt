package com.terkula.uaxctf.training.request.heartrate

class HeartRatePointRequest(
    val offsetSeconds: Int,
    val bpm: Int
)

class SaveHeartRateSeriesRequest(
    val activityType: String,
    val activityUuid: String,
    val runnerId: Int,
    val durationSeconds: Int?,
    val source: String?,
    val samples: List<HeartRatePointRequest>
)
