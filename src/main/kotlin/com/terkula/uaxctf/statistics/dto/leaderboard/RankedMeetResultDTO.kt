package com.terkula.uaxctf.statistics.dto.leaderboard

import com.fasterxml.jackson.annotation.JsonProperty
import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.statistics.dto.MeetPerformanceDTO

class RankedMeetResultDTO(val runner: Runner, @JsonProperty("result") val meetResult: MeetPerformanceDTO, val rank: Int)