package com.terkula.uaxctf.training.response

import com.fasterxml.jackson.annotation.JsonInclude
import java.util.*

class TrainingRunResponse(val trainingRuns: List<TrainingRunDTO>)

@JsonInclude(JsonInclude.Include.NON_NULL)
class TrainingRunDTO(
    val date: Date,
    val distance: Double?,
    val time: String?,
    val icon: String,
    val uuid: String,
    val name: String,
    val minTime: String?,
    val minDistance: Double?,
    val season: String
)