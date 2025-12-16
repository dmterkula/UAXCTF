package com.terkula.uaxctf.statistics.repository

import com.terkula.uaxctf.statisitcs.model.Suggestion
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface SuggestionRepository : CrudRepository<Suggestion, Int> {
    fun findByUuid(uuid: String): Suggestion?
    fun findByRunnerId(runnerId: Int): List<Suggestion>
    fun findByStatus(status: String): List<Suggestion>
    fun findByCategory(category: String): List<Suggestion>
    fun findByTeam(team: String): List<Suggestion>
    fun findByTeamOrderByCreatedAtDesc(team: String): List<Suggestion>
    fun deleteByUuid(uuid: String)
}
