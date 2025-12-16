package com.terkula.uaxctf.statistics.request

data class CreateSuggestionRequest(
    val uuid: String,
    val title: String,
    val description: String,
    val category: String,
    val runnerId: Int,
    val team: String,
    val season: String?
)

data class UpdateSuggestionStatusRequest(
    val suggestionUuid: String,
    val status: String
)

data class ToggleThumbsUpRequest(
    val suggestionUuid: String,
    val runnerId: Int
)

data class CreateSuggestionCommentRequest(
    val uuid: String,
    val suggestionUuid: String,
    val username: String,
    val displayName: String,
    val message: String
)
