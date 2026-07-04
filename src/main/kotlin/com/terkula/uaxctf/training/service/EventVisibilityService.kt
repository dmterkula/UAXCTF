package com.terkula.uaxctf.training.service

import com.terkula.uaxctf.training.model.EventVisibility
import com.terkula.uaxctf.training.repository.EventVisibilityRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class EventVisibilityService(
    private val eventVisibilityRepository: EventVisibilityRepository
) {
    fun getVisibility(eventUuid: String): List<Int> =
        eventVisibilityRepository.findByEventUuid(eventUuid).map { it.runnerId }

    @Transactional
    fun setVisibility(eventUuid: String, runnerIds: List<Int>) {
        eventVisibilityRepository.deleteByEventUuid(eventUuid)
        runnerIds.forEach { runnerId ->
            eventVisibilityRepository.save(EventVisibility(eventUuid, runnerId))
        }
    }

    fun buildVisibilityMap(eventUuids: List<String>): Map<String, List<Int>> =
        eventVisibilityRepository.findByEventUuidIn(eventUuids)
            .groupBy { it.eventUuid }
            .mapValues { (_, entries) -> entries.map { it.runnerId } }
}
