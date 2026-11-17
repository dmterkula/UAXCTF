package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statistics.request.AssignRunnerRequest
import com.terkula.uaxctf.statistics.request.CreateDraftTeamRequest
import com.terkula.uaxctf.statistics.request.UpdateDraftTeamRequest
import com.terkula.uaxctf.statistics.response.*
import com.terkula.uaxctf.statistics.service.DraftService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Coach Improvement Draft game.
 *
 * Base path: /api/v1/draft
 *
 * Coaches snake-draft runners onto their teams, then each team is scored by the
 * mean percent improvement of its drafted athletes from their tryout time trial
 * (scaled to a 5k) to their current season best.
 */
@RestController
@RequestMapping("/api/v1/draft")
class DraftController(val draftService: DraftService) {

    @GetMapping
    fun getTeams(
        @RequestParam season: String,
        @RequestParam year: String
    ): ResponseEntity<List<DraftTeamDTO>> {
        return ResponseEntity.ok(draftService.getTeams(season, year))
    }

    @PostMapping
    fun createTeam(@RequestBody request: CreateDraftTeamRequest): ResponseEntity<DraftTeamDTO> {
        return ResponseEntity.ok(draftService.createTeam(request))
    }

    @PutMapping("/{id}")
    fun updateTeam(
        @PathVariable id: Long,
        @RequestBody request: UpdateDraftTeamRequest
    ): ResponseEntity<DraftTeamDTO> {
        return try {
            ResponseEntity.ok(draftService.updateTeam(id, request))
        } catch (e: RuntimeException) {
            ResponseEntity.notFound().build()
        }
    }

    @DeleteMapping("/{id}")
    fun deleteTeam(@PathVariable id: Long): ResponseEntity<Void> {
        return try {
            draftService.deleteTeam(id)
            ResponseEntity.ok().build()
        } catch (e: RuntimeException) {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/standings")
    fun getStandings(
        @RequestParam season: String,
        @RequestParam year: String
    ): ResponseEntity<DraftStandingsResponse> {
        return ResponseEntity.ok(draftService.getStandings(season, year))
    }

    @GetMapping("/history")
    fun getHistory(
        @RequestParam season: String
    ): ResponseEntity<DraftHistoryResponse> {
        return ResponseEntity.ok(draftService.getHistory(season))
    }

    @GetMapping("/roster-assignment")
    fun getRosterAssignment(
        @RequestParam season: String,
        @RequestParam year: String
    ): ResponseEntity<List<DraftRosterEntryDTO>> {
        return ResponseEntity.ok(draftService.getRosterAssignment(season, year))
    }

    @PostMapping("/{id}/members")
    fun assignRunner(
        @PathVariable id: Long,
        @RequestBody request: AssignRunnerRequest
    ): ResponseEntity<Any> {
        return try {
            draftService.assignRunner(id, request.runnerId)
            ResponseEntity.ok().build()
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        } catch (e: RuntimeException) {
            ResponseEntity.notFound().build()
        }
    }

    @DeleteMapping("/{id}/members/{runnerId}")
    fun removeRunner(
        @PathVariable id: Long,
        @PathVariable runnerId: Int
    ): ResponseEntity<Void> {
        return try {
            draftService.removeRunner(id, runnerId)
            ResponseEntity.ok().build()
        } catch (e: RuntimeException) {
            ResponseEntity.notFound().build()
        }
    }
}
