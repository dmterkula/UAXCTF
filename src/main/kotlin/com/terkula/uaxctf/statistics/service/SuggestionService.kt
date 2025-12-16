package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.statistics.repository.SuggestionCommentRepository
import com.terkula.uaxctf.statistics.repository.SuggestionRepository
import com.terkula.uaxctf.statistics.repository.SuggestionThumbsUpRepository
import com.terkula.uaxctf.statistics.request.CreateSuggestionCommentRequest
import com.terkula.uaxctf.statistics.request.CreateSuggestionRequest
import com.terkula.uaxctf.statistics.request.ToggleThumbsUpRequest
import com.terkula.uaxctf.statistics.request.UpdateSuggestionStatusRequest
import com.terkula.uaxctf.statistics.response.SuggestionDTO
import com.terkula.uaxctf.statistics.response.SuggestionDetailResponse
import com.terkula.uaxctf.statistics.response.SuggestionsListResponse
import com.terkula.uaxctf.statistics.response.ThumbsUpResponse
import com.terkula.uaxctf.statisitcs.model.Suggestion
import com.terkula.uaxctf.statisitcs.model.SuggestionComment
import com.terkula.uaxctf.statisitcs.model.SuggestionThumbsUp
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp

@Service
class SuggestionService(
    val suggestionRepository: SuggestionRepository,
    val suggestionThumbsUpRepository: SuggestionThumbsUpRepository,
    val suggestionCommentRepository: SuggestionCommentRepository,
    val runnerRepository: RunnerRepository
) {

    @Transactional
    fun createSuggestion(request: CreateSuggestionRequest): SuggestionDTO {
        val runner = runnerRepository.findById(request.runnerId)
            .orElseThrow { IllegalArgumentException("Runner not found: ${request.runnerId}") }

        val suggestion = Suggestion(
            uuid = request.uuid,
            title = request.title,
            description = request.description,
            category = request.category,
            runnerId = request.runnerId,
            status = "under_review",
            team = request.team,
            season = request.season
        )

        val saved = suggestionRepository.save(suggestion)
        return toDTO(saved, runner, 0L, false, 0)
    }

    fun getSuggestions(team: String, runnerId: Int? = null): SuggestionsListResponse {
        val suggestions = suggestionRepository.findByTeamOrderByCreatedAtDesc(team)
        val dtos = suggestions.map { suggestion ->
            val runner = runnerRepository.findById(suggestion.runnerId).orElse(null)
            val thumbsUpCount = suggestionThumbsUpRepository.countBySuggestionUuid(suggestion.uuid)
            val hasThumbsUp = runnerId?.let {
                suggestionThumbsUpRepository.findBySuggestionUuidAndRunnerId(suggestion.uuid, it) != null
            }
            val commentCount = suggestionCommentRepository.findBySuggestionUuidOrderByCreatedAtAsc(suggestion.uuid).size
            toDTO(suggestion, runner, thumbsUpCount, hasThumbsUp, commentCount)
        }
        return SuggestionsListResponse(dtos)
    }

    fun getSuggestionDetail(uuid: String, runnerId: Int? = null): SuggestionDetailResponse {
        val suggestion = suggestionRepository.findByUuid(uuid)
            ?: throw IllegalArgumentException("Suggestion not found: $uuid")

        val runner = runnerRepository.findById(suggestion.runnerId).orElse(null)
        val thumbsUpCount = suggestionThumbsUpRepository.countBySuggestionUuid(uuid)
        val hasThumbsUp = runnerId?.let {
            suggestionThumbsUpRepository.findBySuggestionUuidAndRunnerId(uuid, it) != null
        }
        val comments = suggestionCommentRepository.findBySuggestionUuidOrderByCreatedAtAsc(uuid)
        val commentCount = comments.size

        return SuggestionDetailResponse(
            suggestion = toDTO(suggestion, runner, thumbsUpCount, hasThumbsUp, commentCount),
            comments = comments
        )
    }

    @Transactional
    fun updateStatus(request: UpdateSuggestionStatusRequest): SuggestionDTO {
        val suggestion = suggestionRepository.findByUuid(request.suggestionUuid)
            ?: throw IllegalArgumentException("Suggestion not found: ${request.suggestionUuid}")

        val validStatuses = listOf("under_review", "under_consideration", "in_progress", "complete", "rejected")
        if (!validStatuses.contains(request.status)) {
            throw IllegalArgumentException("Invalid status: ${request.status}")
        }

        suggestion.status = request.status
        suggestion.statusChangedAt = Timestamp(System.currentTimeMillis())

        val saved = suggestionRepository.save(suggestion)
        val runner = runnerRepository.findById(saved.runnerId).orElse(null)
        val thumbsUpCount = suggestionThumbsUpRepository.countBySuggestionUuid(saved.uuid)
        val commentCount = suggestionCommentRepository.findBySuggestionUuidOrderByCreatedAtAsc(saved.uuid).size

        return toDTO(saved, runner, thumbsUpCount, false, commentCount)
    }

    @Transactional
    fun toggleThumbsUp(request: ToggleThumbsUpRequest): ThumbsUpResponse {
        val existing = suggestionThumbsUpRepository.findBySuggestionUuidAndRunnerId(
            request.suggestionUuid,
            request.runnerId
        )

        if (existing != null) {
            suggestionThumbsUpRepository.deleteBySuggestionUuidAndRunnerId(
                request.suggestionUuid,
                request.runnerId
            )
            val count = suggestionThumbsUpRepository.countBySuggestionUuid(request.suggestionUuid)
            return ThumbsUpResponse(request.suggestionUuid, count, false)
        } else {
            val thumbsUp = SuggestionThumbsUp(
                suggestionUuid = request.suggestionUuid,
                runnerId = request.runnerId
            )
            suggestionThumbsUpRepository.save(thumbsUp)
            val count = suggestionThumbsUpRepository.countBySuggestionUuid(request.suggestionUuid)
            return ThumbsUpResponse(request.suggestionUuid, count, true)
        }
    }

    @Transactional
    fun addComment(request: CreateSuggestionCommentRequest): SuggestionComment {
        val suggestion = suggestionRepository.findByUuid(request.suggestionUuid)
            ?: throw IllegalArgumentException("Suggestion not found: ${request.suggestionUuid}")

        val comment = SuggestionComment(
            uuid = request.uuid,
            suggestionUuid = request.suggestionUuid,
            username = request.username,
            displayName = request.displayName,
            message = request.message
        )

        return suggestionCommentRepository.save(comment)
    }

    @Transactional
    fun deleteSuggestion(uuid: String) {
        suggestionThumbsUpRepository.deleteBySuggestionUuid(uuid)
        suggestionCommentRepository.deleteBySuggestionUuid(uuid)
        suggestionRepository.deleteByUuid(uuid)
    }

    private fun toDTO(
        suggestion: Suggestion,
        runner: com.terkula.uaxctf.statisitcs.model.Runner?,
        thumbsUpCount: Long,
        hasThumbsUp: Boolean?,
        commentCount: Int
    ): SuggestionDTO {
        return SuggestionDTO(
            uuid = suggestion.uuid,
            title = suggestion.title,
            description = suggestion.description,
            category = suggestion.category,
            runner = runner!!,
            status = suggestion.status,
            createdAt = suggestion.createdAt,
            statusChangedAt = suggestion.statusChangedAt,
            thumbsUpCount = thumbsUpCount,
            hasThumbsUp = hasThumbsUp,
            commentCount = commentCount
        )
    }
}
