package com.terkula.uaxctf.training.response.trainingdashboard

import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.training.model.TrainingRunResult
import com.terkula.uaxctf.training.response.MeetLogResponse
import com.terkula.uaxctf.training.response.RunnerWorkoutResultResponse
import com.terkula.uaxctf.training.response.crosstraining.CrossTrainingRecordProfileResponse
import com.terkula.uaxctf.training.response.track.TrackMeetLogResponse

class IndividualTrainingDashboardResponse(
        val runner: Runner,
        val trainingRunResults: List<TrainingRunResult>,
        val workoutResults: List<RunnerWorkoutResultResponse>,
        val crossTrainingRecords: List<CrossTrainingRecordProfileResponse>,
        val xcMeetLogs: List<MeetLogResponse>,
        val trackMeetLogs: List<TrackMeetLogResponse>
) {
}