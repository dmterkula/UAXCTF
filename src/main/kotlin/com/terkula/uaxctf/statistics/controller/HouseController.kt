package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statistics.request.AssignRunnerRequest
import com.terkula.uaxctf.statistics.request.CreateHouseRequest
import com.terkula.uaxctf.statistics.request.UpdateHouseRequest
import com.terkula.uaxctf.statistics.response.*
import com.terkula.uaxctf.statistics.service.HouseService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * REST controller for the House Points system.
 *
 * Base path: /api/v1/houses
 *
 * Endpoints:
 * - GET /           List houses for a season/year (open)
 * - POST /          Create a house (admin)
 * - PUT /{id}       Rename a house (admin)
 * - DELETE /{id}    Delete a house + its memberships (admin)
 * - GET /standings  House standings with aggregated points (open)
 * - GET /roster-assignment  All runners with their house assignment (admin)
 * - POST /{id}/members      Assign a runner to a house (admin)
 * - DELETE /{id}/members/{runnerId}  Remove a runner from a house (admin)
 * - GET /runner/{runnerId}  Get a specific runner's house (open)
 */
@RestController
@RequestMapping("/api/v1/houses")
class HouseController(val houseService: HouseService) {

    @GetMapping
    fun getHouses(
        @RequestParam season: String,
        @RequestParam year: String
    ): ResponseEntity<List<HouseDTO>> {
        return ResponseEntity.ok(houseService.getHouses(season, year))
    }

    @PostMapping
    fun createHouse(@RequestBody request: CreateHouseRequest): ResponseEntity<HouseDTO> {
        return ResponseEntity.ok(houseService.createHouse(request))
    }

    @PutMapping("/{id}")
    fun updateHouse(
        @PathVariable id: Long,
        @RequestBody request: UpdateHouseRequest
    ): ResponseEntity<HouseDTO> {
        return try {
            ResponseEntity.ok(houseService.updateHouse(id, request))
        } catch (e: RuntimeException) {
            ResponseEntity.notFound().build()
        }
    }

    @DeleteMapping("/{id}")
    fun deleteHouse(@PathVariable id: Long): ResponseEntity<Void> {
        return try {
            houseService.deleteHouse(id)
            ResponseEntity.ok().build()
        } catch (e: RuntimeException) {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/standings")
    fun getStandings(
        @RequestParam season: String,
        @RequestParam year: String
    ): ResponseEntity<HouseStandingsResponse> {
        return ResponseEntity.ok(houseService.getStandings(season, year))
    }

    @GetMapping("/roster-assignment")
    fun getRosterAssignment(
        @RequestParam season: String,
        @RequestParam year: String
    ): ResponseEntity<List<HouseRosterEntryDTO>> {
        return ResponseEntity.ok(houseService.getRosterAssignment(season, year))
    }

    @PostMapping("/{id}/members")
    fun assignRunner(
        @PathVariable id: Long,
        @RequestBody request: AssignRunnerRequest
    ): ResponseEntity<Any> {
        return try {
            houseService.assignRunner(id, request)
            ResponseEntity.ok().build()
        } catch (e: RuntimeException) {
            ResponseEntity.notFound().build()
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }

    @DeleteMapping("/{id}/members/{runnerId}")
    fun removeRunner(
        @PathVariable id: Long,
        @PathVariable runnerId: Int
    ): ResponseEntity<Void> {
        return try {
            houseService.removeRunner(id, runnerId)
            ResponseEntity.ok().build()
        } catch (e: RuntimeException) {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/{id}/members-with-points")
    fun getHouseMembersWithPoints(
        @PathVariable id: Long,
        @RequestParam season: String,
        @RequestParam year: String
    ): ResponseEntity<List<HouseMemberPointsDTO>> {
        return ResponseEntity.ok(houseService.getHouseMembersWithPoints(id, season, year))
    }

    @GetMapping("/runner/{runnerId}")
    fun getRunnerHouse(
        @PathVariable runnerId: Int,
        @RequestParam season: String,
        @RequestParam year: String
    ): ResponseEntity<RunnerHouseResponse> {
        return ResponseEntity.ok(houseService.getRunnerHouse(runnerId, season, year))
    }
}
