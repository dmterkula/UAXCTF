package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statisitcs.model.MeetDivision
import com.terkula.uaxctf.statistics.service.MeetDivisionService
import org.springframework.web.bind.annotation.*

data class CreateMeetDivisionRequest(val meetId: Int, val name: String, val startTime: java.sql.Time? = null, val distanceMeters: Int? = null)
data class UpdateMeetDivisionRequest(val name: String?, val startTime: java.sql.Time? = null, val distanceMeters: Int? = null)
data class ReorderMeetDivisionsRequest(val meetId: Int, val orderedDivisionIds: List<Int>)

@RestController
class MeetDivisionController(val meetDivisionService: MeetDivisionService) {

    @RequestMapping(value = ["xc/meetDivisions"], method = [RequestMethod.GET])
    fun getDivisions(@RequestParam(value = "meetId", required = true) meetId: Int): List<MeetDivision> =
        meetDivisionService.getDivisionsForMeet(meetId)

    @RequestMapping(value = ["xc/meetDivisions/create"], method = [RequestMethod.POST])
    fun createDivision(@RequestBody request: CreateMeetDivisionRequest): MeetDivision =
        meetDivisionService.createDivision(request.meetId, request.name, request.startTime, request.distanceMeters)

    @RequestMapping(value = ["xc/meetDivisions/update/{id}"], method = [RequestMethod.PUT])
    fun updateDivision(@PathVariable id: Int, @RequestBody request: UpdateMeetDivisionRequest): MeetDivision? =
        meetDivisionService.updateDivision(id, request.name, request.startTime, request.distanceMeters)

    @RequestMapping(value = ["xc/meetDivisions/reorder"], method = [RequestMethod.PUT])
    fun reorderDivisions(@RequestBody request: ReorderMeetDivisionsRequest): List<MeetDivision> =
        meetDivisionService.reorderDivisions(request.meetId, request.orderedDivisionIds)

    @RequestMapping(value = ["xc/meetDivisions/delete/{id}"], method = [RequestMethod.DELETE])
    fun deleteDivision(@PathVariable id: Int) = meetDivisionService.deleteDivision(id)
}
