package com.terkula.uaxctf.training.request

import java.sql.Date

class CreateTrainingRunRequest(
        val uuid: String,
        val date: Date,
        val time: String?,
        val distance: Double?,
        val icon: String,
        var name: String,
        var minTime: String?,
        var minDistance: Double?
)

class CreateRunnersTrainingRunRequest(
    val uuid: String,
    val trainingRunUUID: String,
    val runnerId: Int,
    val time: String,
    val distance: Double,
    val avgPace: String,
    val notes: String?,
    val warmUpTime: String?,
    val warmUpPace: String?,
    val warmUpDistance: Double?,
    val coachNotes: String?

)