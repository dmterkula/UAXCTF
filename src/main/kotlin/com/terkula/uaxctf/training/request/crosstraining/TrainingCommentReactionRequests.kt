package com.terkula.uaxctf.training.request.crosstraining

data class AddTrainingCommentReactionRequest(
    val commentUuid: String,
    val reactorUsername: String,
    val reactorDisplayName: String,
    val emoji: String
)

data class RemoveTrainingCommentReactionRequest(
    val commentUuid: String,
    val reactorUsername: String,
    val reactorDisplayName: String,
    val emoji: String
)
