package com.terkula.uaxctf.training.request.journal

import com.fasterxml.jackson.annotation.JsonFormat
import java.sql.Date
import java.sql.Timestamp


class CreateDailyJournalEntryRequest(
        val uuid: String,
        val runnerId: Int,
        val date: Date,
        val text: String,
        val title: String,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
        val timestamp: Timestamp,
        val tagUuids: List<String>,
        val whatIfStatement: String?,
        val favorite: Boolean
) {
}