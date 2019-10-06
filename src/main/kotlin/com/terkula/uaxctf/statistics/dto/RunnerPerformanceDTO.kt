package com.terkula.uaxctf.statistics.dto

import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.statisitcs.model.XCMeetPerformance

class RunnerPerformanceDTO(val runner: Runner, val performance: List<MeetPerformanceDTO>)