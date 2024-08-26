package com.terkula.uaxctf.statistics.repository

import com.terkula.uaxctf.training.model.journal.JournalComment
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface JournalCommentRepository : CrudRepository<JournalComment, Int> {

//    fun findByDateAndRunnerId(date: Date, runnerId: Int): List<DailyJournalEntry>
//
//    fun findByUuid(uuid: String): List<DailyJournalEntry>

    fun findByJournalUuid(journalUuid: String): List<JournalComment>
}