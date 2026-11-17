package com.terkula.uaxctf.statistics.request

import java.sql.Date

class CreateMeetRequest(
    val name: String,
    val date: Date,
    val uuid: String,
    val icon: String,
    var team: String,
    val venueId: Int? = null,
    val startTime: java.sql.Time? = null,
    val courseProfileId: Int? = null,
    // Nullable so an older client that omits the field leaves the stored value alone on update,
    // rather than silently clearing it. Absent on create means false.
    val excludedFromStats: Boolean? = null
)
