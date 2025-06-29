package com.terkula.uaxctf.training.response.runnerseasontrainingcount

import com.terkula.uaxctf.statisitcs.model.Runner

class RunnerSeasonTrainingCount(
        val runner: Runner,
        val trainingCount: Int,
        val trainingMiles: Double,
        val trainingThreshold: Int,
        val milesThreshold: Double
) {
}