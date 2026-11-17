package com.terkula.uaxctf.statistics.repository

import com.terkula.uaxctf.statisitcs.model.DraftTeamMembership
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface DraftTeamMembershipRepository : CrudRepository<DraftTeamMembership, Long> {
    fun findByTeamId(teamId: Long): List<DraftTeamMembership>
    fun findByTeamIdIn(teamIds: List<Long>): List<DraftTeamMembership>
    fun findByRunnerIdAndTeamIdIn(runnerId: Int, teamIds: List<Long>): List<DraftTeamMembership>
    fun deleteByTeamIdAndRunnerId(teamId: Long, runnerId: Int)
    fun deleteByTeamId(teamId: Long)
}
