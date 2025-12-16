package com.terkula.uaxctf.statistics.repository

import com.terkula.uaxctf.statisitcs.model.TeamTalkView
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface TeamTalkViewRepository : CrudRepository<TeamTalkView, Int> {

    /**
     * Get all views for a specific team talk, ordered by most recent first
     */
    fun findByTeamTalkUuidOrderByViewedAtDesc(teamTalkUuid: String): List<TeamTalkView>

    /**
     * Count total views for a specific team talk
     */
    fun countByTeamTalkUuid(teamTalkUuid: String): Long

    /**
     * Get all views by a specific user for a specific team talk
     */
    fun findByTeamTalkUuidAndUsername(teamTalkUuid: String, username: String): List<TeamTalkView>

    /**
     * Count views by a specific user for a specific team talk
     */
    fun countByTeamTalkUuidAndUsername(teamTalkUuid: String, username: String): Long
}
