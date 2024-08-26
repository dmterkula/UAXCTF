package com.terkula.uaxctf.statistics.response.journal

import com.terkula.uaxctf.training.model.journal.DailyJournalEntry
import com.terkula.uaxctf.training.model.journal.JournalComment
import com.terkula.uaxctf.training.model.tags.Tag

class DailyJournalEntryResponse(
        val journalEntry: DailyJournalEntry,
        val journalComments: List<JournalComment>,
        val tags: List<Tag>
)
