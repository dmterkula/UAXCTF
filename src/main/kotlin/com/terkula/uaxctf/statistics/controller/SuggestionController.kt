package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statistics.request.CreateSuggestionCommentRequest
import com.terkula.uaxctf.statistics.request.CreateSuggestionRequest
import com.terkula.uaxctf.statistics.request.ToggleThumbsUpRequest
import com.terkula.uaxctf.statistics.request.UpdateSuggestionStatusRequest
import com.terkula.uaxctf.statistics.response.SuggestionDTO
import com.terkula.uaxctf.statistics.response.SuggestionDetailResponse
import com.terkula.uaxctf.statistics.response.SuggestionsListResponse
import com.terkula.uaxctf.statistics.response.ThumbsUpResponse
import com.terkula.uaxctf.statistics.service.SuggestionService
import com.terkula.uaxctf.statisitcs.model.SuggestionComment
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class SuggestionController(
    val suggestionService: SuggestionService
) {

    @PostMapping("/suggestions")
    fun createSuggestion(
        @RequestBody request: CreateSuggestionRequest
    ): SuggestionDTO {
        return suggestionService.createSuggestion(request)
    }

    @GetMapping("/suggestions")
    fun getSuggestions(
        @RequestParam(value = "team") team: String,
        @RequestParam(value = "runnerId", required = false) runnerId: Int?
    ): SuggestionsListResponse {
        return suggestionService.getSuggestions(team, runnerId)
    }

    @GetMapping("/suggestions/{uuid}")
    fun getSuggestionDetail(
        @PathVariable uuid: String,
        @RequestParam(value = "runnerId", required = false) runnerId: Int?
    ): SuggestionDetailResponse {
        return suggestionService.getSuggestionDetail(uuid, runnerId)
    }

    @PutMapping("/suggestions/status")
    fun updateStatus(
        @RequestBody request: UpdateSuggestionStatusRequest
    ): SuggestionDTO {
        return suggestionService.updateStatus(request)
    }

    @PostMapping("/suggestions/thumbs-up")
    fun toggleThumbsUp(
        @RequestBody request: ToggleThumbsUpRequest
    ): ThumbsUpResponse {
        return suggestionService.toggleThumbsUp(request)
    }

    @PostMapping("/suggestions/comments")
    fun addComment(
        @RequestBody request: CreateSuggestionCommentRequest
    ): SuggestionComment {
        return suggestionService.addComment(request)
    }

    @DeleteMapping("/suggestions/{uuid}")
    fun deleteSuggestion(
        @PathVariable uuid: String
    ) {
        suggestionService.deleteSuggestion(uuid)
    }
}
