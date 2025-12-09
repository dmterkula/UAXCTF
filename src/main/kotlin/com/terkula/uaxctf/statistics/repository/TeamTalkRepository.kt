package com.terkula.uaxctf.statistics.repository

import com.terkula.uaxctf.statisitcs.model.TeamTalk
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface TeamTalkRepository : CrudRepository<TeamTalk, Int> {
    fun findByUuid(uuid: String): TeamTalk?
    fun findBySeasonAndYearOrderByCreatedAtDesc(season: String, year: String): List<TeamTalk>
    fun findBySeasonAndYearAndTeamOrderByCreatedAtDesc(season: String, year: String, team: String): List<TeamTalk>
    fun findAllByOrderByCreatedAtDesc(): List<TeamTalk>
}
