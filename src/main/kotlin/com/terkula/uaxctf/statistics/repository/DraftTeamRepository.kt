package com.terkula.uaxctf.statistics.repository

import com.terkula.uaxctf.statisitcs.model.DraftTeam
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface DraftTeamRepository : CrudRepository<DraftTeam, Long> {
    fun findBySeasonAndYear(season: String, year: String): List<DraftTeam>
    fun findBySeason(season: String): List<DraftTeam>
}
