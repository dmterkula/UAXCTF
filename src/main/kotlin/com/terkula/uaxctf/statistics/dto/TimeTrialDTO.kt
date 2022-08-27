package com.terkula.uaxctf.statistics.dto

import com.terkula.uaxctf.statisitcs.model.Runner

class TimeTrialDTO (val runner: Runner, val time: String, val place: Int, val season: String)

class TimeTrialDifferenceDTO(val runner: Runner, val timeDifference: String, val results: List<TimeTrialDTO>)