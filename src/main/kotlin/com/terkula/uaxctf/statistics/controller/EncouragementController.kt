package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statistics.request.CreateEncouragementReactionRequest
import com.terkula.uaxctf.statistics.request.CreateEncouragementRequest
import com.terkula.uaxctf.statistics.request.SaveEncouragementRequest
import com.terkula.uaxctf.statistics.response.EncouragementDTO
import com.terkula.uaxctf.statistics.response.EncouragementListResponse
import com.terkula.uaxctf.statistics.response.EncouragementReactionDTO
import com.terkula.uaxctf.statistics.service.EncouragementService
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@Validated
@RequestMapping("/api/v1/encouragements")
class EncouragementController(
    val encouragementService: EncouragementService
) {

    @RequestMapping(value = [""], method = [RequestMethod.GET])
    fun getEncouragements(
        @RequestParam(value = "team", required = true) team: String
    ): ResponseEntity<EncouragementListResponse> {
        val response = encouragementService.getActiveEncouragementsByTeam(team)
        return ResponseEntity.ok(response)
    }

    @RequestMapping(value = ["/runner/{runnerId}"], method = [RequestMethod.GET])
    fun getEncouragementsForRunner(
        @PathVariable runnerId: Int
    ): ResponseEntity<EncouragementListResponse> {
        val response = encouragementService.getActiveEncouragementsForRunner(runnerId)
        return ResponseEntity.ok(response)
    }

    @RequestMapping(value = ["/saved/{runnerId}"], method = [RequestMethod.GET])
    fun getSavedEncouragements(
        @PathVariable runnerId: Int
    ): ResponseEntity<EncouragementListResponse> {
        val response = encouragementService.getSavedEncouragements(runnerId)
        return ResponseEntity.ok(response)
    }

    @RequestMapping(value = [""], method = [RequestMethod.POST])
    fun createEncouragement(
        @RequestBody request: CreateEncouragementRequest
    ): ResponseEntity<EncouragementDTO> {
        val response = encouragementService.createEncouragement(request)
        return ResponseEntity.ok(response)
    }

    @RequestMapping(value = ["/{uuid}"], method = [RequestMethod.DELETE])
    fun deleteEncouragement(
        @PathVariable uuid: String
    ): ResponseEntity<Void> {
        encouragementService.deleteEncouragement(uuid)
        return ResponseEntity.ok().build()
    }

    @RequestMapping(value = ["/reactions"], method = [RequestMethod.POST])
    fun createReaction(
        @RequestBody request: CreateEncouragementReactionRequest
    ): ResponseEntity<EncouragementReactionDTO> {
        val response = encouragementService.createReaction(request)
        return ResponseEntity.ok(response)
    }

    @RequestMapping(value = ["/reactions/{uuid}"], method = [RequestMethod.DELETE])
    fun deleteReaction(
        @PathVariable uuid: String
    ): ResponseEntity<Void> {
        encouragementService.deleteReaction(uuid)
        return ResponseEntity.ok().build()
    }

    @RequestMapping(value = ["/save"], method = [RequestMethod.POST])
    fun saveEncouragement(
        @RequestBody request: SaveEncouragementRequest
    ): ResponseEntity<Void> {
        encouragementService.saveEncouragement(request)
        return ResponseEntity.ok().build()
    }

    @RequestMapping(value = ["/save/{encouragementUuid}/{runnerId}"], method = [RequestMethod.DELETE])
    fun unsaveEncouragement(
        @PathVariable encouragementUuid: String,
        @PathVariable runnerId: Int
    ): ResponseEntity<Void> {
        encouragementService.unsaveEncouragement(encouragementUuid, runnerId)
        return ResponseEntity.ok().build()
    }
}
