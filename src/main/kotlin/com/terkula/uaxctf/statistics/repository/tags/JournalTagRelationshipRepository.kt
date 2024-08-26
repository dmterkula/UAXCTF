package com.terkula.uaxctf.statistics.repository.tags

import com.terkula.uaxctf.training.model.tags.JournalTagRelationship
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface JournalTagRelationshipRepository: CrudRepository<JournalTagRelationship, Int> {

    fun findByJournalUuid(journalUuid: String): List<JournalTagRelationship>

    fun deleteByJournalUuidAndTagUuid(journalUuid: String, tagUuid: String): List<JournalTagRelationship>

    fun deleteByRunnerIdAndTagUuid(runnerId: Int?, tagUuid: String): List<JournalTagRelationship>

}