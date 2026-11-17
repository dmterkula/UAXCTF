package com.terkula.uaxctf.training.repository

import com.terkula.uaxctf.training.model.EventGroup
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface EventGroupRepository : CrudRepository<EventGroup, Int> {

    fun findByEventUuid(eventUuid: String): List<EventGroup>

    fun findByEventUuidIn(eventUuids: List<String>): List<EventGroup>

    fun deleteByEventUuid(eventUuid: String)
}
