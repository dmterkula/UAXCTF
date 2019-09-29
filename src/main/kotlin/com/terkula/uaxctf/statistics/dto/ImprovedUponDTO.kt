package com.terkula.uaxctf.statistics.dto

import com.fasterxml.jackson.annotation.JsonPropertyOrder

@JsonPropertyOrder("meet")
class ImprovedUponDTO(val timeDifference: String, val meet: MeetPerformanceDTO?)

