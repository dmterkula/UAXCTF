package com.terkula.uaxctf.statistics.repository

import com.terkula.uaxctf.training.model.TrainingRun
import com.terkula.uaxctf.training.model.journal.DailyJournalEntry
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.sql.Date

@Repository
interface DailyJournalRepository : CrudRepository<DailyJournalEntry, Int> {

    fun findByDateAndRunnerId(date: Date, runnerId: Int): List<DailyJournalEntry>

    fun findByUuid(uuid: String): List<DailyJournalEntry>

    fun findByDateBetweenAndRunnerId(start: Date, end: Date, runnerId: Int) : List<DailyJournalEntry>

    fun findByTitleContainingAndDateBetweenAndRunnerId(title: String, start: Date, end: Date, runnerId: Int): List<DailyJournalEntry>


}
