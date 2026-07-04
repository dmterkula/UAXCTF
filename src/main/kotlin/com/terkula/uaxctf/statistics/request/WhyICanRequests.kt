package com.terkula.uaxctf.statistics.request

data class CreateWhyICanRequest(
    val uuid: String,
    val runnerId: Int,
    val text: String,
    val size: Int = 3
)

data class UpdateWhyICanRequest(
    val uuid: String,
    val text: String,
    val size: Int
)

data class CreateWhyICanCommentRequest(
    val uuid: String,
    val whyICanRunnerId: Int,
    val parentCommentUuid: String? = null,
    val username: String,
    val displayName: String,
    val deviceId: String? = null,
    val message: String
)

data class CreateWhyICanActivityLinkRequest(
    val uuid: String,
    val runnerId: Int,
    val activityType: String,
    val activityUuid: String,
    val displayTitle: String,
    val activityDate: String
)

data class ReorderWhyICanActivityLinksRequest(
    val runnerId: Int,
    val orderedUuids: List<String>
)
