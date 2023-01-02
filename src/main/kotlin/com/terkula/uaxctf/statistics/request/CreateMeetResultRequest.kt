package com.terkula.uaxctf.statistics.request

class CreateMeetResultRequest(
    var season: String,
    var runnerId: Int,
    var meetName: String,
    var time: String,
    var place: Int,
    var passesLastMile: Int,
    var passesSecondMile: Int,
    var skullsEarned: Int,
    var mileOneSplit: String,
    var mileTwoSplit: String,
    var mileThreeSplit: String
)