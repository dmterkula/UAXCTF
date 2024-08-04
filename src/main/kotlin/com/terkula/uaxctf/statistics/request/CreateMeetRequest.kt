package com.terkula.uaxctf.statistics.request

import java.sql.Date

class CreateMeetRequest(
    val name: String,
    val date: Date,
    val uuid: String,
    val icon: String,
    var team: String
)