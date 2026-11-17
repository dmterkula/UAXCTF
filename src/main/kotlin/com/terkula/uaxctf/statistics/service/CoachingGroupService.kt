package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statisitcs.model.CoachingGroup
import com.terkula.uaxctf.statisitcs.model.CoachingGroupMembership
import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.statistics.repository.CoachingGroupMembershipRepository
import com.terkula.uaxctf.statistics.repository.CoachingGroupRepository
import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.statistics.request.AssignRunnerToGroupRequest
import com.terkula.uaxctf.statistics.request.CopyCoachingGroupsRequest
import com.terkula.uaxctf.statistics.request.CreateCoachingGroupRequest
import com.terkula.uaxctf.statistics.request.UpdateCoachingGroupRequest
import com.terkula.uaxctf.statistics.response.CoachingGroupDTO
import com.terkula.uaxctf.statistics.response.RunnerCoachingGroupDTO
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CoachingGroupService(
    val coachingGroupRepository: CoachingGroupRepository,
    val coachingGroupMembershipRepository: CoachingGroupMembershipRepository,
    val runnerRepository: RunnerRepository
) {

    fun getCoachingGroups(year: String): List<CoachingGroupDTO> {
        val groups = coachingGroupRepository.findByYear(year)
        if (groups.isEmpty()) return emptyList()

        val groupIds = groups.map { it.id }
        val membershipsByGroup = coachingGroupMembershipRepository.findByGroupIdIn(groupIds)
            .groupBy { it.groupId }

        val allRunnerIds = membershipsByGroup.values.flatten().map { it.runnerId }.distinct()
        val runnerMap = allRunnerIds.mapNotNull { runnerRepository.findById(it).orElse(null) }
            .associateBy { it.id }

        return groups.map { group ->
            val members = (membershipsByGroup[group.id] ?: emptyList())
                .mapNotNull { runnerMap[it.runnerId] }
                .sortedBy { it.lastNameKey() }
            group.toDTO(members)
        }.sortedBy { it.name.lowercase() }
    }

    @Transactional
    fun createCoachingGroup(request: CreateCoachingGroupRequest): CoachingGroupDTO {
        val group = CoachingGroup(
            name = request.name,
            year = request.year,
            coachUsername = request.coachUsername,
            createdBy = request.createdBy
        )
        return coachingGroupRepository.save(group).toDTO(emptyList())
    }

    @Transactional
    fun updateCoachingGroup(id: Long, request: UpdateCoachingGroupRequest): CoachingGroupDTO {
        val group = coachingGroupRepository.findById(id)
            .orElseThrow { RuntimeException("Coaching group not found: $id") }
        group.name = request.name
        group.coachUsername = request.coachUsername
        val saved = coachingGroupRepository.save(group)
        val members = coachingGroupMembershipRepository.findByGroupId(id)
            .mapNotNull { runnerRepository.findById(it.runnerId).orElse(null) }
            .sortedBy { it.lastNameKey() }
        return saved.toDTO(members)
    }

    @Transactional
    fun deleteCoachingGroup(id: Long) {
        if (!coachingGroupRepository.existsById(id)) {
            throw RuntimeException("Coaching group not found: $id")
        }
        coachingGroupMembershipRepository.deleteByGroupId(id)
        coachingGroupRepository.deleteById(id)
    }

    /**
     * Assign a runner to a group, enforcing one-group-per-year by MOVING the runner:
     * any existing membership in a sibling group (same year) is removed first.
     */
    @Transactional
    fun assignRunner(groupId: Long, request: AssignRunnerToGroupRequest) {
        val group = coachingGroupRepository.findById(groupId)
            .orElseThrow { RuntimeException("Coaching group not found: $groupId") }

        val siblingGroupIds = coachingGroupRepository.findByYear(group.year).map { it.id }
        val existing = coachingGroupMembershipRepository
            .findByRunnerIdAndGroupIdIn(request.runnerId, siblingGroupIds)

        if (existing.any { it.groupId == groupId }) return // already in target

        existing.forEach {
            coachingGroupMembershipRepository.deleteByGroupIdAndRunnerId(it.groupId, request.runnerId)
        }
        coachingGroupMembershipRepository.save(
            CoachingGroupMembership(groupId = groupId, runnerId = request.runnerId)
        )
    }

    @Transactional
    fun removeRunner(groupId: Long, runnerId: Int) {
        if (!coachingGroupRepository.existsById(groupId)) {
            throw RuntimeException("Coaching group not found: $groupId")
        }
        coachingGroupMembershipRepository.deleteByGroupIdAndRunnerId(groupId, runnerId)
    }

    fun getRunnerCoachingGroup(runnerId: Int, year: String): RunnerCoachingGroupDTO {
        val groups = coachingGroupRepository.findByYear(year)
        val groupIds = groups.map { it.id }
        val membership = if (groupIds.isEmpty()) null
                         else coachingGroupMembershipRepository
                             .findByRunnerIdAndGroupIdIn(runnerId, groupIds).firstOrNull()
        val group = membership?.let { m -> groups.firstOrNull { it.id == m.groupId } }
        return RunnerCoachingGroupDTO(
            runnerId = runnerId,
            groupId = group?.id,
            groupName = group?.name,
            coachUsername = group?.coachUsername,
            year = year
        )
    }

    /**
     * Seed a new year's coaching groups from a prior year: copies each group (name + coach)
     * and its runner assignments. No-op if the target year already has groups.
     */
    @Transactional
    fun copyCoachingGroups(request: CopyCoachingGroupsRequest): List<CoachingGroupDTO> {
        if (coachingGroupRepository.findByYear(request.toYear).isNotEmpty()) {
            return getCoachingGroups(request.toYear)
        }
        val sourceGroups = coachingGroupRepository.findByYear(request.fromYear)
        sourceGroups.forEach { source ->
            val newGroup = coachingGroupRepository.save(
                CoachingGroup(
                    name = source.name,
                    year = request.toYear,
                    coachUsername = source.coachUsername,
                    createdBy = request.createdBy
                )
            )
            coachingGroupMembershipRepository.findByGroupId(source.id).forEach { membership ->
                coachingGroupMembershipRepository.save(
                    CoachingGroupMembership(groupId = newGroup.id, runnerId = membership.runnerId)
                )
            }
        }
        return getCoachingGroups(request.toYear)
    }

    private fun Runner.lastNameKey(): String =
        name.trim().split(" ").last().lowercase()

    private fun CoachingGroup.toDTO(members: List<Runner>) = CoachingGroupDTO(
        id = id,
        name = name,
        year = year,
        coachUsername = coachUsername,
        members = members
    )
}
