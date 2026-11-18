package com.terkula.uaxctf.training.response.heartrate

import com.fasterxml.jackson.annotation.JsonInclude

class HeartRatePointDTO(
    val offsetSeconds: Int,
    val bpm: Int
)

@JsonInclude(JsonInclude.Include.NON_NULL)
class HeartRateSeriesResponse(
    val activityType: String,
    val activityUuid: String,
    val runnerId: Int,
    val durationSeconds: Int,
    val avgHr: Int?,
    val maxHr: Int?,
    val minHr: Int?,
    val source: String?,
    val samples: List<HeartRatePointDTO>
)
