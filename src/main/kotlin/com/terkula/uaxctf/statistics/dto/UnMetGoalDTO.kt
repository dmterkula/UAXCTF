package com.terkula.uaxctf.statistics.dto

import com.terkula.uaxctf.statisitcs.model.Runner

class UnMetGoalDTO (val runner: Runner, val time: String, val closestPerformance: MeetPerformanceDTO, val difference: String)