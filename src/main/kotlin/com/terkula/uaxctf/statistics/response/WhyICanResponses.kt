package com.terkula.uaxctf.statistics.response

data class WhyICanEntryDTO(
    val uuid: String,
    val runnerId: Int,
    val text: String,
    val size: Int,
    val createdAt: String,
    val updatedAt: String
)

data class WhyICanNestedComment(
    val uuid: String,
    val whyICanRunnerId: Int,
    val username: String,
    val displayName: String,
    val deviceId: String? = null,
    val message: String,
    val createdAt: String,
    val updatedAt: String,
    val replies: List<WhyICanNestedComment> = emptyList()
)

data class WhyICanActivityLinkDTO(
    val uuid: String,
    val runnerId: Int,
    val activityType: String,
    val activityUuid: String,
    val displayTitle: String,
    val activityDate: String,
    val displayOrder: Int,
    val createdAt: String,
    val updatedAt: String
)

data class WhyICanResponse(
    val entries: List<WhyICanEntryDTO>,
    val comments: List<WhyICanNestedComment>,
    val totalCommentCount: Int,
    val activityLinks: List<WhyICanActivityLinkDTO> = emptyList()
)
