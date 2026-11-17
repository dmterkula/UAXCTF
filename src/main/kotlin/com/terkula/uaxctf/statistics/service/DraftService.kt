package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statisitcs.model.DraftTeam
import com.terkula.uaxctf.statisitcs.model.DraftTeamMembership
import com.terkula.uaxctf.statistics.controller.MeetPerformanceController
import com.terkula.uaxctf.statistics.repository.DraftTeamMembershipRepository
import com.terkula.uaxctf.statistics.repository.DraftTeamRepository
import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.statistics.request.CreateDraftTeamRequest
import com.terkula.uaxctf.statistics.request.UpdateDraftTeamRequest
import com.terkula.uaxctf.statistics.response.*
import com.terkula.uaxctf.util.calculateSecondsFrom
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.Date

@Service
class DraftService(
    val draftTeamRepository: DraftTeamRepository,
    val draftTeamMembershipRepository: DraftTeamMembershipRepository,
    val runnerRepository: RunnerRepository,
    val timeTrialService: TimeTrialService
) {

    fun getTeams(season: String, year: String): List<DraftTeamDTO> {
        return draftTeamRepository.findBySeasonAndYear(season, year).map { it.toDTO() }
    }

    @Transactional
    fun createTeam(request: CreateDraftTeamRequest): DraftTeamDTO {
        val team = DraftTeam(
            name = request.name,
            coachName = request.coachName,
            season = request.season,
            year = request.year,
            createdBy = request.createdBy
        )
        return draftTeamRepository.save(team).toDTO()
    }

    @Transactional
    fun updateTeam(id: Long, request: UpdateDraftTeamRequest): DraftTeamDTO {
        val team = draftTeamRepository.findById(id)
            .orElseThrow { RuntimeException("Draft team not found: $id") }
        team.name = request.name
        team.coachName = request.coachName
        return draftTeamRepository.save(team).toDTO()
    }

    @Transactional
    fun deleteTeam(id: Long) {
        if (!draftTeamRepository.existsById(id)) {
            throw RuntimeException("Draft team not found: $id")
        }
        draftTeamMembershipRepository.deleteByTeamId(id)
        draftTeamRepository.deleteById(id)
    }

    @Transactional
    fun assignRunner(teamId: Long, runnerId: Int): DraftTeamMembership {
        val team = draftTeamRepository.findById(teamId)
            .orElseThrow { RuntimeException("Draft team not found: $teamId") }

        // A runner may only be on one team per season/year (snake draft)
        val siblingTeamIds = draftTeamRepository.findBySeasonAndYear(team.season, team.year).map { it.id }
        val existing = draftTeamMembershipRepository.findByRunnerIdAndTeamIdIn(runnerId, siblingTeamIds)
        if (existing.isNotEmpty()) {
            val conflictTeamId = existing.first().teamId
            val conflictName = draftTeamRepository.findById(conflictTeamId).map { it.name }.orElse("unknown")
            throw IllegalArgumentException(
                "Runner $runnerId is already drafted to '$conflictName' for ${team.season} ${team.year}"
            )
        }

        return draftTeamMembershipRepository.save(DraftTeamMembership(teamId = teamId, runnerId = runnerId))
    }

    @Transactional
    fun removeRunner(teamId: Long, runnerId: Int) {
        if (!draftTeamRepository.existsById(teamId)) {
            throw RuntimeException("Draft team not found: $teamId")
        }
        draftTeamMembershipRepository.deleteByTeamIdAndRunnerId(teamId, runnerId)
    }

    fun getRosterAssignment(season: String, year: String): List<DraftRosterEntryDTO> {
        val teams = draftTeamRepository.findBySeasonAndYear(season, year)
        val teamIds = teams.map { it.id }
        val allMemberships = if (teamIds.isEmpty()) emptyList()
                             else draftTeamMembershipRepository.findByTeamIdIn(teamIds)
        val membershipByRunner = allMemberships.associateBy { it.runnerId }
        val teamById = teams.associateBy { it.id }

        val yearInt = year.toIntOrNull() ?: return emptyList()
        val allRunners = if (season.equals("xc", ignoreCase = true)) {
            runnerRepository.findByDoesXc(true)
                .filter { it.graduatingClass.toInt() > yearInt && it.graduatingClass.toInt() <= yearInt + 4 }
                .filter { it.isActive }
        } else {
            runnerRepository.findByDoesTrack(true)
                .filter { it.graduatingClass.toInt() >= yearInt && it.graduatingClass.toInt() < yearInt + 4 }
                .filter { it.isActive }
        }

        return allRunners.map { runner ->
            val membership = membershipByRunner[runner.id]
            val team = membership?.let { teamById[it.teamId] }
            DraftRosterEntryDTO(
                runnerId = runner.id,
                runnerName = runner.name,
                teamId = team?.id,
                teamName = team?.name,
                coachName = team?.coachName
            )
        }.sortedBy { it.runnerName }
    }

    fun getStandings(season: String, year: String): DraftStandingsResponse {
        return DraftStandingsResponse(
            season = season,
            year = year,
            standings = computeStandings(season, year)
        )
    }

    fun getHistory(season: String): DraftHistoryResponse {
        val allTeams = draftTeamRepository.findBySeason(season)
        val years = allTeams.map { it.year }.distinct().sortedDescending()
        val currentYear = MeetPerformanceController.CURRENT_YEAR.toString()

        val yearResults = years.map { year ->
            val standings = computeStandings(season, year)
            val winner = standings.firstOrNull { it.rank == 1 && it.scoredCount > 0 }
            DraftYearResultDTO(
                year = year,
                isCurrent = year == currentYear,
                winningCoach = winner?.coachName,
                winningTeam = winner?.name,
                winningPercent = winner?.avgPercentImprovement,
                standings = standings
            )
        }

        // Aggregate each coach across every year they fielded a scored team.
        data class CoachYear(val year: String, val avg: Double, val won: Boolean)
        val perCoach = mutableMapOf<String, MutableList<CoachYear>>()
        yearResults.forEach { yr ->
            yr.standings.filter { it.scoredCount > 0 }.forEach { team ->
                perCoach.getOrPut(team.coachName) { mutableListOf() }
                    .add(CoachYear(yr.year, team.avgPercentImprovement, team.rank == 1))
            }
        }

        val allTime = perCoach.map { (coach, entries) ->
            val best = entries.maxByOrNull { it.avg }
            Triple(coach, entries, best)
        }.map { (coach, entries, best) ->
            DraftCoachRecordDTO(
                coachName = coach,
                rank = 0,
                championships = entries.count { it.won },
                yearsPlayed = entries.size,
                careerAvgImprovement = round2(entries.sumByDouble { it.avg } / entries.size),
                bestYear = best?.year,
                bestYearPercent = best?.avg
            )
        }.sortedWith(
            compareByDescending<DraftCoachRecordDTO> { it.championships }
                .thenByDescending { it.careerAvgImprovement }
        ).mapIndexed { index, record -> record.copy(rank = index + 1) }

        return DraftHistoryResponse(season = season, years = yearResults, allTime = allTime)
    }

    private fun computeStandings(season: String, year: String): List<DraftTeamStandingDTO> {
        val teams = draftTeamRepository.findBySeasonAndYear(season, year)
        if (teams.isEmpty()) {
            return emptyList()
        }

        val startDate = Date.valueOf("$year-01-01")
        val endDate = Date.valueOf("$year-12-31")

        // Per-runner tryout (scaled to 5k) and current season best, reusing the existing progression calc.
        val improvementByRunner = timeTrialService
            .getRankedProgressionSinceTimeTrial(startDate, endDate, false)
            .associateBy { it.runner.id }

        val teamIds = teams.map { it.id }
        val membershipsByTeam = draftTeamMembershipRepository.findByTeamIdIn(teamIds).groupBy { it.teamId }
        val runnerNames = runnerRepository.findAll().associate { it.id to it.name }

        val unranked = teams.map { team ->
            val memberships = membershipsByTeam[team.id] ?: emptyList()
            val athletes = memberships.map { m ->
                val imp = improvementByRunner[m.runnerId]
                val name = runnerNames[m.runnerId] ?: "Unknown"
                if (imp == null || imp.seasonBest == "00:00" || imp.adjustedTimeTrial == "00:00") {
                    DraftAthleteImprovementDTO(
                        runnerId = m.runnerId,
                        runnerName = name,
                        tryoutTime = imp?.adjustedTimeTrial ?: "--",
                        seasonBest = "--",
                        improvement = "--",
                        percentImprovement = 0.0,
                        hasResult = false
                    )
                } else {
                    val tryoutSeconds = imp.adjustedTimeTrial.calculateSecondsFrom()
                    val sbSeconds = imp.seasonBest.calculateSecondsFrom()
                    // Positive percent = got faster (SB below tryout time).
                    val percent = if (tryoutSeconds > 0) (tryoutSeconds - sbSeconds) / tryoutSeconds * 100.0 else 0.0
                    DraftAthleteImprovementDTO(
                        runnerId = m.runnerId,
                        runnerName = name,
                        tryoutTime = imp.adjustedTimeTrial,
                        seasonBest = imp.seasonBest,
                        improvement = imp.improvement,
                        percentImprovement = round2(percent),
                        hasResult = true
                    )
                }
            }.sortedByDescending { it.percentImprovement }

            val scored = athletes.filter { it.hasResult }
            // Average over ALL drafted athletes: no-result athletes count as 0%,
            // regressions (slower SB) count as their negative percent.
            val avg = if (athletes.isEmpty()) 0.0 else round2(athletes.sumByDouble { it.percentImprovement } / athletes.size)
            DraftTeamStandingDTO(
                id = team.id,
                name = team.name,
                coachName = team.coachName,
                rank = 0,
                avgPercentImprovement = avg,
                memberCount = athletes.size,
                scoredCount = scored.size,
                athletes = athletes
            )
        }

        return unranked
            .sortedByDescending { it.avgPercentImprovement }
            .mapIndexed { index, it -> it.copy(rank = index + 1) }
    }

    private fun round2(value: Double): Double = Math.round(value * 100.0) / 100.0

    private fun DraftTeam.toDTO() = DraftTeamDTO(
        id = id, name = name, coachName = coachName, season = season, year = year
    )
}
