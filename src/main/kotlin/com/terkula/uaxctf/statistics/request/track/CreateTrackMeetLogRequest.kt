package com.terkula.uaxctf.statistics.request.track

class CreateTrackMeetLogRequest(val logs: List<TrackMeetLogCreation>) {

}

class TrackMeetLogCreation(
        val runnerId: Int,
        val meetId: String,
        val notes: String?,
        var time: String,
        var warmUpTime: String?,
        var warmUpDistance: Double?,
        var warmUpPace: String?,
        var coolDownTime: String?,
        var coolDownDistance: Double?,
        var coolDownPace: String?,
        var coachNotes: String?,
        var season: String = "track",
        var event: String
) {

}