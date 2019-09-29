package com.terkula.uaxctf.statistics.response

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.common.collect.ImmutableMap
import com.terkula.uaxctf.statistics.dto.MeetPerformanceDTO
import com.terkula.uaxctf.statisitcs.model.Runner

class RunnerMeetPerformanceResponse {

    @JsonProperty("results")
    var performanceMap: Map<Runner, List<MeetPerformanceDTO>>? = null

    constructor(runner: Runner, performanceList: List<MeetPerformanceDTO>) {
        this.performanceMap = ImmutableMap.of(runner, performanceList)
    }

    constructor(performanceMap: Map<Runner, List<MeetPerformanceDTO>>) {
        this.performanceMap = performanceMap
    }
}
