package com.terkula.uaxctf.statistics.dto

import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.statisitcs.model.XcGoal

open class RunnerGoalDTO(val runner: Runner, val times: List<XcGoal>)

//@JsonPropertyOrder("runner,goalTime,at")
//open class MetGoalDTO(runner: Runner, @JsonProperty("goalTime") time: String, val at: MeetPerformanceDTO): RunnerGoalDTO(runner, listOf(time))

@JsonPropertyOrder("time,at")
class MetGoalPerformanceDTO(val time: String, val at: MeetPerformanceDTO)

open class RunnersMetGoals(val runner: Runner, val metGoals: List<MetGoalPerformanceDTO>)