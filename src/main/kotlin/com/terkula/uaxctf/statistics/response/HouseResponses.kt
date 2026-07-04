package com.terkula.uaxctf.statistics.response

data class HouseDTO(
    val id: Long,
    val name: String,
    val season: String,
    val year: String
)

data class HouseStandingsEntry(
    val id: Long,
    val name: String,
    val totalPoints: Int,
    val memberCount: Int,
    val rank: Int
)

data class HouseStandingsResponse(
    val season: String,
    val year: String,
    val standings: List<HouseStandingsEntry>
)

data class HouseRosterEntryDTO(
    val runnerId: Int,
    val runnerName: String,
    val houseId: Long?,
    val houseName: String?
)

data class RunnerHouseResponse(
    val runnerId: Int,
    val houseId: Long?,
    val houseName: String?,
    val season: String,
    val year: String
)

data class HouseMemberPointsDTO(
    val runnerId: Int,
    val runnerName: String,
    val points: Int
)
