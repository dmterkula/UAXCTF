package com.terkula.uaxctf.statistics.request

import com.fasterxml.jackson.annotation.JsonFormat

data class CreateTeamTalkRequest(
    val uuid: String,
    val author: String,
    val season: String,
    val year: String,
    val title: String,
    val content: String,
    val team: String = "UA"
)

data class UpdateTeamTalkRequest(
    val uuid: String,
    val title: String,
    val content: String
)

data class CreateTeamTalkCommentRequest(
    val uuid: String,
    val teamTalkUuid: String,
    val parentCommentUuid: String? = null,
    val username: String,
    val displayName: String,
    val deviceId: String? = null,
    val message: String,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
    val timestamp: String,
    val season: String,
    val year: String
)

data class AddTeamTalkReactionRequest(
    val teamTalkUuid: String,
    val username: String,
    val displayName: String,
    val emoji: String
)

data class RemoveTeamTalkReactionRequest(
    val teamTalkUuid: String,
    val username: String,
    val displayName: String,
    val emoji: String
)

data class TrackTeamTalkViewRequest(
    val teamTalkUuid: String,
    val username: String,
    val displayName: String,
    val team: String
)
