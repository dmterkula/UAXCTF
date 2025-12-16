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
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class SuggestionController(
    val suggestionService: SuggestionService
) {

    @ApiOperation("Create a new suggestion")
    @PostMapping("/suggestions")
    fun createSuggestion(
        @RequestBody request: CreateSuggestionRequest
    ): SuggestionDTO {
        return suggestionService.createSuggestion(request)
    }

    @ApiOperation("Get all suggestions for a team")
    @GetMapping("/suggestions")
    fun getSuggestions(
        @ApiParam("Team name") @RequestParam team: String,
        @ApiParam("Optional runner ID to check thumbs up status") @RequestParam(required = false) runnerId: Int?
    ): SuggestionsListResponse {
        return suggestionService.getSuggestions(team, runnerId)
    }

    @ApiOperation("Get suggestion detail with comments")
    @GetMapping("/suggestions/{uuid}")
    fun getSuggestionDetail(
        @PathVariable uuid: String,
        @ApiParam("Optional runner ID to check thumbs up status") @RequestParam(required = false) runnerId: Int?
    ): SuggestionDetailResponse {
        return suggestionService.getSuggestionDetail(uuid, runnerId)
    }

    @ApiOperation("Update suggestion status (coaches only)")
    @PutMapping("/suggestions/status")
    fun updateStatus(
        @RequestBody request: UpdateSuggestionStatusRequest
    ): SuggestionDTO {
        return suggestionService.updateStatus(request)
    }

    @ApiOperation("Toggle thumbs up on a suggestion")
    @PostMapping("/suggestions/thumbs-up")
    fun toggleThumbsUp(
        @RequestBody request: ToggleThumbsUpRequest
    ): ThumbsUpResponse {
        return suggestionService.toggleThumbsUp(request)
    }

    @ApiOperation("Add comment to suggestion")
    @PostMapping("/suggestions/comments")
    fun addComment(
        @RequestBody request: CreateSuggestionCommentRequest
    ): SuggestionComment {
        return suggestionService.addComment(request)
    }

    @ApiOperation("Delete suggestion (coaches only)")
    @DeleteMapping("/suggestions/{uuid}")
    fun deleteSuggestion(
        @PathVariable uuid: String
    ) {
        suggestionService.deleteSuggestion(uuid)
    }
}
