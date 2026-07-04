package com.terkula.uaxctf.training.controller

import com.terkula.uaxctf.training.service.EventVisibilityService
import org.springframework.web.bind.annotation.*

data class SetVisibilityRequest(val runnerIds: List<Int>)
data class VisibilityResponse(val eventUuid: String, val runnerIds: List<Int>)

@RestController
@RequestMapping("/api/events")
class EventVisibilityController(
    private val eventVisibilityService: EventVisibilityService
) {

    @GetMapping("/{uuid}/visibility")
    fun getVisibility(@PathVariable uuid: String): VisibilityResponse {
        return VisibilityResponse(uuid, eventVisibilityService.getVisibility(uuid))
    }

    @PutMapping("/{uuid}/visibility")
    fun setVisibility(
        @PathVariable uuid: String,
        @RequestBody request: SetVisibilityRequest
    ): VisibilityResponse {
        eventVisibilityService.setVisibility(uuid, request.runnerIds)
        return VisibilityResponse(uuid, request.runnerIds)
    }
}
