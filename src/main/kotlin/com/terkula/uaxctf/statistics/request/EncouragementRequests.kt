package com.terkula.uaxctf.statistics.request

data class CreateEncouragementRequest(
    val uuid: String,
    val authorRunnerId: Int?,
    val authorUsername: String,
    val authorDisplayName: String,
    val recipientRunnerId: Int?,
    val text: String,
    val team: String
)

data class CreateEncouragementReactionRequest(
    val uuid: String,
    val encouragementUuid: String,
    val reactorUsername: String,
    val reactorDisplayName: String,
    val emoji: String
)

data class SaveEncouragementRequest(
    val encouragementUuid: String,
    val runnerId: Int
)
