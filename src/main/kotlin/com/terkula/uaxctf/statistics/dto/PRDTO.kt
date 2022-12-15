package com.terkula.uaxctf.statistics.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.terkula.uaxctf.statisitcs.model.Runner

class PRDTO (val runner: Runner, @JsonProperty("result") val pr: List<MeetPerformanceDTO>, val improvedUpon: ImprovedUponDTO)

class RankedPRDTO(val runner: Runner, @JsonProperty("result") val pr: MeetPerformanceDTO, val rank: Int)