package com.terkula.uaxctf.statistics.request


class CreateMeetLogRequest(
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
    var season: String = "xc",
    var satisfaction: Int?,
    var happyWith: String?,
    var notHappyWith: String?
)

class CreateXcPreMeetLogRequest(
    val runnerId: Int,
    val meetId: String,
    val uuid: String,
    var goals: String?,
    var plan: String?,
    var confidence: String?,
    var preparation: String?,
    var whenItsHard: String?,
    var questions: String?,
    var notes: String?,
    var sleepScore: Int?,
    var fuelingScore: Int?,
    var hydrationScore: Int?,
    var sorenessScore: Int?,
)