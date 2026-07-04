package com.terkula.uaxctf.statistics.response

import com.terkula.uaxctf.training.model.Firebase.TrainingEvent
import com.terkula.uaxctf.training.model.Firebase.WorkoutEvent
import com.terkula.uaxctf.training.response.MeetLogResponse
import com.terkula.uaxctf.training.response.track.TrackMeetLogResponse
import java.sql.Date

data class OnThisDayEvent(
    val year: Int,
    val date: Date,
    val type: String,
    val title: String,
    val description: String?,
    val uuid: String,
    val icon: String,
    val trainingRunEvent: TrainingEvent? = null,
    val workoutEvent: WorkoutEvent? = null,
    val meetLogResponse: MeetLogResponse? = null,
    val trackMeetLogResponse: TrackMeetLogResponse? = null
)

data class OnThisDayYearGroup(val year: Int, val events: List<OnThisDayEvent>)

data class OnThisDayResponse(val month: Int, val day: Int, val runnerId: Int, val years: List<OnThisDayYearGroup>)
