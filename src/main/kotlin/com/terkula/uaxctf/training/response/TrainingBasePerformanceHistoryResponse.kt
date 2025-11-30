package com.terkula.uaxctf.training.response

import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.training.model.trainingbase.TrainingBasePerformance

class TrainingBasePerformanceHistoryResponse(
        val runner: Runner,
        val history: List<TrainingBasePerformance>
) {
}