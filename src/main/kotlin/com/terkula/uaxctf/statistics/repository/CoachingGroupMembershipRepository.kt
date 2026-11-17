package com.terkula.uaxctf.statistics.repository

import com.terkula.uaxctf.statisitcs.model.CoachingGroupMembership
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CoachingGroupMembershipRepository : CrudRepository<CoachingGroupMembership, Long> {
    fun findByGroupId(groupId: Long): List<CoachingGroupMembership>
    fun findByGroupIdIn(groupIds: List<Long>): List<CoachingGroupMembership>
    fun findByRunnerIdAndGroupIdIn(runnerId: Int, groupIds: List<Long>): List<CoachingGroupMembership>
    fun deleteByGroupIdAndRunnerId(groupId: Long, runnerId: Int)
    fun deleteByGroupIdAndRunnerIdIn(groupId: Long, runnerIds: List<Int>)
    fun deleteByGroupId(groupId: Long)
}
