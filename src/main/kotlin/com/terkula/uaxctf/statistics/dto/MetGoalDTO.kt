package com.terkula.uaxctf.statistics.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.terkula.uaxctf.statisitcs.model.Runner

@JsonPropertyOrder("runner,goalTime,at")
open class MetGoalDTO(runner: Runner, @JsonProperty("goalTime") time: String, val at: MeetPerformanceDTO): RunnerGoalDTO(runner, time)