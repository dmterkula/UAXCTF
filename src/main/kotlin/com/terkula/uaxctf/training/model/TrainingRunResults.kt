package com.terkula.uaxctf.training.model

import com.terkula.uaxctf.training.response.RunnerTrainingRunDTO

class TrainingRunResult(val trainingRun: TrainingRun, val results: List<RunnerTrainingRunDTO>) {
}

class TrainingRunResults(val trainingRunResults: List<TrainingRunResult>)
