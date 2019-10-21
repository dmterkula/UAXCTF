package com.terkula.uaxctf.statistics.dto

import com.terkula.uaxctf.statisitcs.model.Runner

class TimeTrialImprovementDTO (val rank: Int, val runner: Runner, val adjustedTimeTrial: String, val seasonBest: String, val improvement: String)