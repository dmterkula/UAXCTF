package com.terkula.uaxctf.statistics.response

data class EncouragementReactionDTO(
    val uuid: String,
    val encouragementUuid: String,
    val reactorUsername: String,
    val reactorDisplayName: String,
    val emoji: String,
    val createdAt: String
)

data class EncouragementDTO(
    val uuid: String,
    val authorRunnerId: Int?,
    val authorUsername: String,
    val authorDisplayName: String,
    val recipientRunnerId: Int?,
    val recipientName: String?,
    val text: String,
    val team: String,
    val saved: Boolean,
    val reactions: List<EncouragementReactionDTO>,
    val createdAt: String,
    val expiresAt: String
)

data class EncouragementListResponse(
    val encouragements: List<EncouragementDTO>
)
