package com.terkula.uaxctf.statistics.request

data class CreateCoachingGroupRequest(
    val name: String,
    val year: String,
    val coachUsername: String,
    val createdBy: String? = null
)

data class UpdateCoachingGroupRequest(
    val name: String,
    val coachUsername: String
)

data class AssignRunnerToGroupRequest(
    val runnerId: Int
)

data class CopyCoachingGroupsRequest(
    val fromYear: String,
    val toYear: String,
    val createdBy: String? = null
)
