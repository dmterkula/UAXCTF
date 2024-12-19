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