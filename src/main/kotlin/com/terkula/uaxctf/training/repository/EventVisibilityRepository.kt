package com.terkula.uaxctf.training.repository

import com.terkula.uaxctf.training.model.EventVisibility
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface EventVisibilityRepository : CrudRepository<EventVisibility, Int> {

    fun findByEventUuid(eventUuid: String): List<EventVisibility>

    fun findByEventUuidIn(eventUuids: List<String>): List<EventVisibility>

    fun deleteByEventUuid(eventUuid: String)
}
