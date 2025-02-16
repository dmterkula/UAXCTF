package com.terkula.uaxctf.training.response

import com.fasterxml.jackson.annotation.JsonInclude
import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.training.model.TrainingComment
import com.terkula.uaxctf.util.calculateSecondsFrom


class RunnersTrainingRunResponse(val runnerTrainingRuns: List<RunnerTrainingRunDTO>, val comments: List<TrainingComment>)

@JsonInclude(JsonInclude.Include.NON_NULL)
class RunnerTrainingRunDTO(
    val runner: Runner,
    val uuid: String,
    val trainingRunUuid: String,
    val time: String,
    val distance: Double,
    val avgPace: String,
    val notes: String?,
    val warmUpTime: String?,
    val warmUpDistance: Double?,
    val warmUpPace: String?,
    val coachNotes: String?,
    val effortLevel: Double?,
    val paceRange: TrainingRunPaceRange?,
    val painLevel: Double?,
    val painNotes: String?,
    val splits: List<String>?
)

fun RunnerTrainingRunDTO.getTotalDistance(): Double {

    var warmUpDist = 0.0
    if (warmUpDistance != null) {
        warmUpDist = warmUpDistance!!
    }

    return distance + warmUpDist
}

fun RunnerTrainingRunDTO.getTotalTime(): Double {

    var warmUpTimeCounter = 0.0
    if (warmUpTime != null) {
        warmUpTimeCounter = warmUpTime!!.calculateSecondsFrom()
    }

    return time.calculateSecondsFrom() + warmUpTimeCounter
}