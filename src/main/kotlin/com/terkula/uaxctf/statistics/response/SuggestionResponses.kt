package com.terkula.uaxctf.statistics.response

import com.fasterxml.jackson.annotation.JsonInclude
import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.statisitcs.model.SuggestionComment
import java.sql.Timestamp

@JsonInclude(JsonInclude.Include.NON_NULL)
data class SuggestionDTO(
    val uuid: String,
    val title: String,
    val description: String,
    val category: String,
    val runner: Runner,
    val status: String,
    val createdAt: Timestamp,
    val statusChangedAt: Timestamp?,
    val thumbsUpCount: Long,
    val hasThumbsUp: Boolean? = null,
    val commentCount: Int
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class SuggestionDetailResponse(
    val suggestion: SuggestionDTO,
    val comments: List<SuggestionComment>
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class SuggestionsListResponse(
    val suggestions: List<SuggestionDTO>
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ThumbsUpResponse(
    val suggestionUuid: String,
    val thumbsUpCount: Long,
    val hasThumbsUp: Boolean
)
