package com.terkula.uaxctf.statistics.request

data class CreateDraftTeamRequest(
    val name: String,
    val coachName: String,
    val season: String,
    val year: String,
    val createdBy: String? = null
)

data class UpdateDraftTeamRequest(
    val name: String,
    val coachName: String
)
