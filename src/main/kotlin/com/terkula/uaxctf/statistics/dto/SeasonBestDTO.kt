package com.terkula.uaxctf.statistics.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.terkula.uaxctf.statisitcs.model.Runner

class SeasonBestDTO (val runner: Runner, @JsonProperty("result") val seasonBest: List<MeetPerformanceDTO>, val improvedUpon: ImprovedUponDTO)