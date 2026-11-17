package com.terkula.uaxctf.statistics.response

data class DraftTeamDTO(
    val id: Long,
    val name: String,
    val coachName: String,
    val season: String,
    val year: String
)

data class DraftRosterEntryDTO(
    val runnerId: Int,
    val runnerName: String,
    val teamId: Long?,
    val teamName: String?,
    val coachName: String?
)

data class DraftAthleteImprovementDTO(
    val runnerId: Int,
    val runnerName: String,
    val tryoutTime: String,
    val seasonBest: String,
    val improvement: String,
    val percentImprovement: Double,
    val hasResult: Boolean
)

data class DraftTeamStandingDTO(
    val id: Long,
    val name: String,
    val coachName: String,
    val rank: Int,
    val avgPercentImprovement: Double,
    val memberCount: Int,
    val scoredCount: Int,
    val athletes: List<DraftAthleteImprovementDTO>
)

data class DraftStandingsResponse(
    val season: String,
    val year: String,
    val standings: List<DraftTeamStandingDTO>
)

data class DraftYearResultDTO(
    val year: String,
    val isCurrent: Boolean,
    val winningCoach: String?,
    val winningTeam: String?,
    val winningPercent: Double?,
    val standings: List<DraftTeamStandingDTO>
)

data class DraftCoachRecordDTO(
    val coachName: String,
    val rank: Int,
    val championships: Int,
    val yearsPlayed: Int,
    val careerAvgImprovement: Double,
    val bestYear: String?,
    val bestYearPercent: Double?
)

data class DraftHistoryResponse(
    val season: String,
    val years: List<DraftYearResultDTO>,
    val allTime: List<DraftCoachRecordDTO>
)
