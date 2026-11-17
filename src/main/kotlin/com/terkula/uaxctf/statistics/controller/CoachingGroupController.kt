package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statistics.request.AssignRunnerToGroupRequest
import com.terkula.uaxctf.statistics.request.CopyCoachingGroupsRequest
import com.terkula.uaxctf.statistics.request.CreateCoachingGroupRequest
import com.terkula.uaxctf.statistics.request.UpdateCoachingGroupRequest
import com.terkula.uaxctf.statistics.response.CoachingGroupDTO
import com.terkula.uaxctf.statistics.response.RunnerCoachingGroupDTO
import com.terkula.uaxctf.statistics.service.CoachingGroupService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * REST controller for per-year coaching groups.
 *
 * Base path: /api/v1/coaching-groups
 * - GET    /?year=                     List groups (with members) for a year
 * - POST   /                           Create a group
 * - PUT    /{id}                       Update a group's name + coach
 * - DELETE /{id}                       Delete a group + its memberships
 * - POST   /{id}/members               Assign (move) a runner into a group
 * - DELETE /{id}/members/{runnerId}    Remove a runner from a group
 * - GET    /runner/{runnerId}?year=    Get a runner's group for a year
 * - POST   /copy                       Seed a new year's groups from a prior year
 */
@RestController
@RequestMapping("/api/v1/coaching-groups")
class CoachingGroupController(val coachingGroupService: CoachingGroupService) {

    @GetMapping
    fun getCoachingGroups(@RequestParam year: String): ResponseEntity<List<CoachingGroupDTO>> {
        return ResponseEntity.ok(coachingGroupService.getCoachingGroups(year))
    }

    @PostMapping
    fun createCoachingGroup(
        @RequestBody request: CreateCoachingGroupRequest
    ): ResponseEntity<CoachingGroupDTO> {
        return ResponseEntity.ok(coachingGroupService.createCoachingGroup(request))
    }

    @PutMapping("/{id}")
    fun updateCoachingGroup(
        @PathVariable id: Long,
        @RequestBody request: UpdateCoachingGroupRequest
    ): ResponseEntity<CoachingGroupDTO> {
        return try {
            ResponseEntity.ok(coachingGroupService.updateCoachingGroup(id, request))
        } catch (e: RuntimeException) {
            ResponseEntity.notFound().build()
        }
    }

    @DeleteMapping("/{id}")
    fun deleteCoachingGroup(@PathVariable id: Long): ResponseEntity<Void> {
        return try {
            coachingGroupService.deleteCoachingGroup(id)
            ResponseEntity.ok().build()
        } catch (e: RuntimeException) {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping("/{id}/members")
    fun assignRunner(
        @PathVariable id: Long,
        @RequestBody request: AssignRunnerToGroupRequest
    ): ResponseEntity<Any> {
        return try {
            coachingGroupService.assignRunner(id, request)
            ResponseEntity.ok().build()
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
            coachingGroupService.removeRunner(id, runnerId)
            ResponseEntity.ok().build()
        } catch (e: RuntimeException) {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/runner/{runnerId}")
    fun getRunnerCoachingGroup(
        @PathVariable runnerId: Int,
        @RequestParam year: String
    ): ResponseEntity<RunnerCoachingGroupDTO> {
        return ResponseEntity.ok(coachingGroupService.getRunnerCoachingGroup(runnerId, year))
    }

    @PostMapping("/copy")
    fun copyCoachingGroups(
        @RequestBody request: CopyCoachingGroupsRequest
    ): ResponseEntity<List<CoachingGroupDTO>> {
        return ResponseEntity.ok(coachingGroupService.copyCoachingGroups(request))
    }
}
