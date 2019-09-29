package com.terkula.uaxctf.statistics.response

import com.fasterxml.jackson.annotation.JsonProperty
import com.terkula.uaxctf.statistics.dto.MeetProgressionDTO

class MeetProgressionResponse(
        val count: Int,
        @JsonProperty("results")
        var performanceMap: List<MeetProgressionDTO>)