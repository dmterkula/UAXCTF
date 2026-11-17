package com.terkula.uaxctf.statistics.request

import java.sql.Date

data class DismissReplyQueueItemRequest(
    val coachUsername: String,
    val entityUuid: String,
    val entityType: String,
    val logDate: Date
)

/** Bulk "clear everything I'm never going to get to" -- dismisses all items older than [beforeDate]. */
data class DismissReplyQueueBeforeDateRequest(
    val coachUsername: String,
    val beforeDate: Date,
    val season: String,
    val team: String? = null
)
