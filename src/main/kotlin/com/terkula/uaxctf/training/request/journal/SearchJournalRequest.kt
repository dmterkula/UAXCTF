package com.terkula.uaxctf.training.request.journal

import java.sql.Date

class SearchJournalRequest(
        val runnerId: Int,
        val title: String?,
        val startDate: Date,
        val endDate: Date,
        val tagUuids: List<String>,
        val andTags: Boolean,
        val searchFavorites: Boolean
) {
}