package com.terkula.uaxctf.statistics.dto.leaderboard

import com.fasterxml.jackson.annotation.JsonProperty
import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.statistics.dto.ImprovedUponDTO
import com.terkula.uaxctf.statistics.dto.MeetPerformanceDTO

class SeasonBestDTO (val runner: Runner, @JsonProperty("result") val seasonBest: List<MeetPerformanceDTO>, val improvedUpon: ImprovedUponDTO)

class RankedSeasonBestDTO (val runner: Runner, @JsonProperty("result") val seasonBest: MeetPerformanceDTO, val rank: Int)