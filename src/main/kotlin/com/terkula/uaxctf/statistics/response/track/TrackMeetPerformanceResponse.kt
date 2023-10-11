package com.terkula.uaxctf.statistics.response.track

class TrackMeetPerformanceResponse (
        val meetId: String,
        val uuid: String,
        val runnerId: Int,
        var time: String,
        var place: Int,
        var event: String,
        var isSplit: Boolean,
        var splits: List<TrackSplit>
)

data class TrackSplit(var splitName: String, var splitValue: String)