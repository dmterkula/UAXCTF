package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statistics.request.*
import com.terkula.uaxctf.statistics.response.*
import com.terkula.uaxctf.statistics.service.TeamTalkService
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@RestController
@Validated
@RequestMapping("/api/v1/team-talks")
class TeamTalkController(
    val teamTalkService: TeamTalkService
) {

    // ===== TEAM TALK CRUD =====

    @RequestMapping(value = [""], method = [RequestMethod.POST])
    fun createTeamTalk(
        @RequestBody @Valid request: CreateTeamTalkRequest
    ): ResponseEntity<TeamTalkResponse> {
        val response = teamTalkService.createTeamTalk(request)
        return ResponseEntity.ok(response)
    }

    @RequestMapping(value = [""], method = [RequestMethod.PUT])
    fun updateTeamTalk(
        @RequestBody @Valid request: UpdateTeamTalkRequest
    ): ResponseEntity<TeamTalkResponse> {
        val response = teamTalkService.updateTeamTalk(request)
        return ResponseEntity.ok(response)
    }

    @RequestMapping(value = ["/{uuid}"], method = [RequestMethod.GET])
    fun getTeamTalk(
        @PathVariable uuid: String
    ): ResponseEntity<TeamTalkResponse> {
        val response = teamTalkService.getTeamTalk(uuid)
        return ResponseEntity.ok(response)
    }

    @RequestMapping(value = ["/season"], method = [RequestMethod.GET])
    fun getTeamTalksBySeason(
        @RequestParam season: String,
        @RequestParam year: String,
        @RequestParam(required = false, defaultValue = "UA") team: String,
        @RequestParam(required = false, defaultValue = "false") includeDrafts: Boolean
    ): ResponseEntity<TeamTalkListResponse> {
        val response = teamTalkService.getTeamTalksBySeason(season, year, team, includeDrafts)
        return ResponseEntity.ok(response)
    }

    @RequestMapping(value = ["/all"], method = [RequestMethod.GET])
    fun getAllTeamTalks(
        @RequestParam(required = false, defaultValue = "false") includeDrafts: Boolean
    ): ResponseEntity<TeamTalkListResponse> {
        val response = teamTalkService.getAllTeamTalks(includeDrafts)
        return ResponseEntity.ok(response)
    }

    @RequestMapping(value = ["/{uuid}/publish"], method = [RequestMethod.POST])
    fun publishTeamTalk(
        @PathVariable uuid: String,
        @RequestBody @Valid request: PublishTeamTalkRequest
    ): ResponseEntity<PublishTeamTalkResponse> {
        val response = teamTalkService.publishTeamTalk(uuid, request.author)
        return ResponseEntity.ok(response)
    }

    @RequestMapping(value = ["/{uuid}/unpublish"], method = [RequestMethod.POST])
    fun unpublishTeamTalk(
        @PathVariable uuid: String,
        @RequestBody @Valid request: PublishTeamTalkRequest
    ): ResponseEntity<TeamTalkResponse> {
        val response = teamTalkService.unpublishTeamTalk(uuid, request.author)
        return ResponseEntity.ok(response)
    }

    // ===== REACTIONS =====

    @RequestMapping(value = ["/reactions/add"], method = [RequestMethod.POST])
    fun addReaction(
        @RequestBody @Valid request: AddTeamTalkReactionRequest
    ): ResponseEntity<List<ReactionSummary>> {
        val reactions = teamTalkService.addReaction(request)
        return ResponseEntity.ok(reactions)
    }

    @RequestMapping(value = ["/reactions/remove"], method = [RequestMethod.POST])
    fun removeReaction(
        @RequestBody @Valid request: RemoveTeamTalkReactionRequest
    ): ResponseEntity<List<ReactionSummary>> {
        val reactions = teamTalkService.removeReaction(request)
        return ResponseEntity.ok(reactions)
    }

    // ===== COMMENTS =====

    @RequestMapping(value = ["/comments"], method = [RequestMethod.POST])
    fun createComment(
        @RequestBody @Valid request: CreateTeamTalkCommentRequest
    ): ResponseEntity<CommentCreatedResponse> {
        val response = teamTalkService.createComment(request)
        return ResponseEntity.ok(response)
    }

    // ===== VIEW TRACKING =====

    @RequestMapping(value = ["/views/track"], method = [RequestMethod.POST])
    fun trackView(
        @RequestBody @Valid request: TrackTeamTalkViewRequest
    ): ResponseEntity<Void> {
        teamTalkService.trackView(request)
        return ResponseEntity.ok().build()
    }
}
