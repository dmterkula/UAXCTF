package com.terkula.uaxctf.training.response

import java.sql.Date

data class CoachTrainingRunInsightsDTO(
    val startDate: Date,
    val endDate: Date,
    val runners: List<RunnerTrainingInsightDTO>
)

data class RunnerTrainingInsightDTO(
    val runnerId: Int,
    val runnerName: String,
    val loggedRunCount: Int,
    val longestRun: LongestRunDTO?,
    val paceByEffort: Map<String, String>
)

data class LongestRunDTO(
    val date: Date,
    val distance: Double,
    val durationMinutes: Int,
    val avgPace: String
)
