package com.terkula.uaxctf.training.response

import com.terkula.uaxctf.statisitcs.model.Runner

class TrainingRunPaceRange(val baseTrainingEvent: String,
                           val fastEndPercent: Int,
                           val fastEndPace: String,
                           val slowEndPercent: Int,
                           val slowEndPace: String
) {
}

class RunnersTrainingRunPaceRange(val runner: Runner, val trainingRunPaceRange: TrainingRunPaceRange?) {
}

class RunnerBaseTrainingPercentageResponse (
    val runner: Runner,
    val event: String,
    val paceType: String,
    val paceName: String,
    val percent: Int,
    val season: String,
    val year: String,
    val uuid: String
) {

}