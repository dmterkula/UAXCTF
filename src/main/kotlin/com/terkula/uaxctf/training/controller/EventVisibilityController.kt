package com.terkula.uaxctf.training.controller

import com.terkula.uaxctf.training.service.EventGroupService
import com.terkula.uaxctf.training.service.EventVisibilityService
import org.springframework.web.bind.annotation.*

data class SetVisibilityRequest(val runnerIds: List<Int>)
data class VisibilityResponse(val eventUuid: String, val runnerIds: List<Int>)
data class SetGroupsRequest(val groups: List<String>)
data class GroupsResponse(val eventUuid: String, val groups: List<String>)

@RestController
@RequestMapping("/api/events")
class EventVisibilityController(
    private val eventVisibilityService: EventVisibilityService,
    private val eventGroupService: EventGroupService
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

    @GetMapping("/{uuid}/groups")
    fun getGroups(@PathVariable uuid: String): GroupsResponse {
        return GroupsResponse(uuid, eventGroupService.getGroups(uuid))
    }

    @PutMapping("/{uuid}/groups")
    fun setGroups(
        @PathVariable uuid: String,
        @RequestBody request: SetGroupsRequest
    ): GroupsResponse {
        return GroupsResponse(uuid, eventGroupService.setGroups(uuid, request.groups))
    }
}
