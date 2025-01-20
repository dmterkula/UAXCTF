package com.terkula.uaxctf.training.response.trainingdashboard

import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.training.response.MeetLogResponse
import com.terkula.uaxctf.training.response.RunnerTrainingRunDTO
import com.terkula.uaxctf.training.response.RunnerWorkoutResultResponse
import com.terkula.uaxctf.training.response.crosstraining.CrossTrainingRecordProfileResponse
import com.terkula.uaxctf.training.response.track.TrackMeetLogResponse

class RunnerTrainingDashboardEntry(
        val runner: Runner,
        val weeklyRuns: Int,
        val weeklyMiles: Double,
        val weeklyAvgPace: String,
        val weeklyTrainingRunAvgEffort: Double,
        val trainingRuns: List<RunnerTrainingRunDTO>,
        val workouts: List<RunnerWorkoutResultResponse>,
        val crossTrainingRecords: List<CrossTrainingRecordProfileResponse>,
        val xcMeetLogs: List<MeetLogResponse>,
        val trackMeetLogs: List<TrackMeetLogResponse>
    ) {
}