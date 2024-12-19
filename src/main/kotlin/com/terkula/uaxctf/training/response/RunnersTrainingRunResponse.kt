package com.terkula.uaxctf.training.response

import com.fasterxml.jackson.annotation.JsonInclude
import com.terkula.uaxctf.statisitcs.model.Runner


class RunnersTrainingRunResponse(val runnerTrainingRuns: List<RunnerTrainingRunDTO>)

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
    val paceRange: TrainingRunPaceRange?
)