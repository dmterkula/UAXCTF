package com.terkula.uaxctf.statistics.request.track

class CreateTrackMeetResultRequest(
    var season: String,
    var runnerId: Int,
    var meetName: String,
    var meetId: String,
    var results: List<TrackResult>
)

class TrackResult(
    var uuid: String,
    var event: String,
    var time: String,
    var place: Int,
    var splits: List<MeetSplit>,
    var isSplit: Boolean
)

class MeetSplit(
    var uuid: String,
    var splitName: String,
    var splitValue: String
)