package com.terkula.uaxctf.statistics.response

import com.terkula.uaxctf.statisitcs.model.Runner

data class CoachingGroupDTO(
    val id: Long,
    val name: String,
    val year: String,
    val coachUsername: String,
    val members: List<Runner>
)

data class RunnerCoachingGroupDTO(
    val runnerId: Int,
    val groupId: Long?,
    val groupName: String?,
    val coachUsername: String?,
    val year: String
)
