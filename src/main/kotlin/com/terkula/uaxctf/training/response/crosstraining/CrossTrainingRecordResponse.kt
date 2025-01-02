package com.terkula.uaxctf.training.response.crosstraining

import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.training.model.CrossTraining.CrossTraining
import com.terkula.uaxctf.training.model.CrossTraining.CrossTrainingRecord
import com.terkula.uaxctf.training.model.TrainingComment

class CrossTrainingRecordResponse(
        val runner: Runner,
        val crossTrainingRecord: CrossTrainingRecord,
        val comments: List<TrainingComment>) {
}

class CrossTrainingRecordProfileResponse(
        val crossTrainingEvent: CrossTraining,
        val crossTrainingRecord: CrossTrainingRecord
) {
}