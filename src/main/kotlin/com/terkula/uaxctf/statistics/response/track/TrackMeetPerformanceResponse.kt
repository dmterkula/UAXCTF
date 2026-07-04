package com.terkula.uaxctf.statistics.response.track

import com.fasterxml.jackson.annotation.JsonProperty
import java.sql.Date

class TrackMeetPerformanceResponse (
        val meetId: String,
        val meetName: String,
        val meetDate: Date,
        val uuid: String,
        val runnerId: Int,
        var time: String,
        var place: Int,
        var event: String,
        @param:JsonProperty("split")
        @get:JsonProperty("split")
        var isSplit: Boolean,
        var splits: List<TrackSplit>
)

data class TrackSplit(var splitName: String, var splitValue: String)