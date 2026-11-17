package com.terkula.uaxctf.training.service

import com.terkula.uaxctf.training.model.EventGroup
import com.terkula.uaxctf.training.repository.EventGroupRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class EventGroupService(
    private val eventGroupRepository: EventGroupRepository
) {
    companion object {
        val VALID_GROUPS = setOf("distance", "mid-distance", "400")
    }

    fun getGroups(eventUuid: String): List<String> =
        eventGroupRepository.findByEventUuid(eventUuid).map { it.trackGroup }

    /** Replaces the event's group tags. An empty list means the event is for all groups. */
    @Transactional
    fun setGroups(eventUuid: String, groups: List<String>): List<String> {
        val cleaned = groups.map { it.trim().lowercase() }.filter { it in VALID_GROUPS }.distinct()
        eventGroupRepository.deleteByEventUuid(eventUuid)
        cleaned.forEach { eventGroupRepository.save(EventGroup(eventUuid, it)) }
        return cleaned
    }

    /** uuid -> groups, omitting events with no tags (null means "all groups"). */
    fun buildGroupsMap(eventUuids: List<String>): Map<String, List<String>> =
        if (eventUuids.isEmpty()) emptyMap()
        else eventGroupRepository.findByEventUuidIn(eventUuids)
            .groupBy { it.eventUuid }
            .mapValues { (_, entries) -> entries.map { it.trackGroup } }
}
