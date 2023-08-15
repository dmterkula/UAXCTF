package com.terkula.uaxctf.statistics.request

import com.terkula.uaxctf.training.request.ComponentsSplitsRequest

class CreateMeetLogRequest(
    val runnerId: Int,
    val meetId: Int,
    val notes: String?,
    var time: String,
    var warmUpTime: String?,
    var warmUpDistance: Double?,
    var warmUpPace: String?,
    var coolDownTime: String?,
    var coolDownDistance: Double?,
    var coolDownPace: String?
)