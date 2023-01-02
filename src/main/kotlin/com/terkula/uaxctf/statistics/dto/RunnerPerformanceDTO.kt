package com.terkula.uaxctf.statistics.dto

import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.statisitcs.model.XCMeetPerformance
import java.sql.Date

class RunnerPerformanceDTO(val runner: Runner, val performance: List<MeetPerformanceDTO>)

class RunnerMeetPerformanceDTO(val runner: Runner, val performance: TotalMeetPerformanceDTO)

class TotalMeetPerformanceDTO(
    var meetName: String,
    var meetDate: Date,
    var time: String,
    var place: Int,
    var mileOneSplit: String,
    var mileTwoSplit: String,
    var mileThreeSplit: String,
    var passesLastMile: Int = 0,
    var passesSecondMile: Int = 0,
    var skullsEarned: Int = 0
)