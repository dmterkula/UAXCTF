package com.terkula.uaxctf.statistics.request

data class CreateHouseRequest(
    val name: String,
    val season: String,
    val year: String,
    val createdBy: String? = null
)

data class UpdateHouseRequest(
    val name: String
)

data class AssignRunnerRequest(
    val runnerId: Int
)
