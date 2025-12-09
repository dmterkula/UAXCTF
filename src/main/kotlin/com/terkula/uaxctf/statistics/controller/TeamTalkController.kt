package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statistics.request.*
import com.terkula.uaxctf.statistics.response.*
import com.terkula.uaxctf.statistics.service.TeamTalkService
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
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

    @ApiOperation("Create a new team talk (coaches only)")
    @RequestMapping(value = [""], method = [RequestMethod.POST])
    fun createTeamTalk(
        @RequestBody @Valid request: CreateTeamTalkRequest
    ): ResponseEntity<TeamTalkResponse> {
        val response = teamTalkService.createTeamTalk(request)
        return ResponseEntity.ok(response)
    }

    @ApiOperation("Update an existing team talk (coaches only)")
    @RequestMapping(value = [""], method = [RequestMethod.PUT])
    fun updateTeamTalk(
        @RequestBody @Valid request: UpdateTeamTalkRequest
    ): ResponseEntity<TeamTalkResponse> {
        val response = teamTalkService.updateTeamTalk(request)
        return ResponseEntity.ok(response)
    }

    @ApiOperation("Get a specific team talk by UUID with all reactions and nested comments")
    @RequestMapping(value = ["/{uuid}"], method = [RequestMethod.GET])
    fun getTeamTalk(
        @ApiParam("Team talk UUID")
        @PathVariable uuid: String
    ): ResponseEntity<TeamTalkResponse> {
        val response = teamTalkService.getTeamTalk(uuid)
        return ResponseEntity.ok(response)
    }

    @ApiOperation("Get all team talks for a specific season and year")
    @RequestMapping(value = ["/season"], method = [RequestMethod.GET])
    fun getTeamTalksBySeason(
        @ApiParam("Season (xc or track)")
        @RequestParam season: String,
        @ApiParam("Year (e.g., 2024)")
        @RequestParam year: String,
        @ApiParam("Team (default UA)")
        @RequestParam(required = false, defaultValue = "UA") team: String
    ): ResponseEntity<TeamTalkListResponse> {
        val response = teamTalkService.getTeamTalksBySeason(season, year, team)
        return ResponseEntity.ok(response)
    }

    @ApiOperation("Get all team talks across all seasons")
    @RequestMapping(value = ["/all"], method = [RequestMethod.GET])
    fun getAllTeamTalks(): ResponseEntity<TeamTalkListResponse> {
        val response = teamTalkService.getAllTeamTalks()
        return ResponseEntity.ok(response)
    }

    // ===== REACTIONS =====

    @ApiOperation("Add an emoji reaction to a team talk")
    @RequestMapping(value = ["/reactions/add"], method = [RequestMethod.POST])
    fun addReaction(
        @RequestBody @Valid request: AddTeamTalkReactionRequest
    ): ResponseEntity<List<ReactionSummary>> {
        val reactions = teamTalkService.addReaction(request)
        return ResponseEntity.ok(reactions)
    }

    @ApiOperation("Remove an emoji reaction from a team talk")
    @RequestMapping(value = ["/reactions/remove"], method = [RequestMethod.POST])
    fun removeReaction(
        @RequestBody @Valid request: RemoveTeamTalkReactionRequest
    ): ResponseEntity<List<ReactionSummary>> {
        val reactions = teamTalkService.removeReaction(request)
        return ResponseEntity.ok(reactions)
    }

    // ===== COMMENTS =====

    @ApiOperation("Add a comment to a team talk (awards 3 pride points to the athlete)")
    @RequestMapping(value = ["/comments"], method = [RequestMethod.POST])
    fun createComment(
        @RequestBody @Valid request: CreateTeamTalkCommentRequest
    ): ResponseEntity<CommentCreatedResponse> {
        val response = teamTalkService.createComment(request)
        return ResponseEntity.ok(response)
    }
}
