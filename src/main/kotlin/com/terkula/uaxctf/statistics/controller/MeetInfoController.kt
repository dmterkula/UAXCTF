package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statisitcs.model.Meet
import com.terkula.uaxctf.statistics.request.CreateMeetRequest
import com.terkula.uaxctf.statistics.response.MeetResponse
import com.terkula.uaxctf.statistics.response.track.TrackAndXcMeetResponse
import com.terkula.uaxctf.statistics.service.MeetInfoService
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.web.bind.annotation.*

import java.sql.Date

@RestController
class MeetInfoController(var meetInfoService: MeetInfoService) {

    @ApiOperation("Returns a List of meet names to select for results ")
    @RequestMapping(value = ["xc/getMeetNames"], method = [RequestMethod.GET])
    fun getMeets() : MeetResponse {

       return MeetResponse(meetInfoService.getMeetInfo())
    }

    @ApiOperation("Returns a List of meet names to select for results for both XC and track")
    @RequestMapping(value = ["/getAllMeetNames"], method = [RequestMethod.GET])
    fun getAllMeets() : TrackAndXcMeetResponse {
        return TrackAndXcMeetResponse(meetInfoService.getMeetInfo(), meetInfoService.getTrackMeetInfo())
    }

    @ApiOperation("Returns a List of meet names to select for results ")
    @RequestMapping(value = ["xc/meets/getBySeason"], method = [RequestMethod.GET])
    fun getMeetsForSeason(
        @ApiParam("season")
        @RequestParam(value = "season", required = true) season: String,
    ) : List<Meet> {

        return meetInfoService.getMeetsForSeason(season)
    }

    @ApiOperation("Returns a List of meet names to select for results ")
    @RequestMapping(value = ["xc/meets/getByDates"], method = [RequestMethod.GET])
    fun getMeetsBetweenDates(
            @ApiParam("startDate")
            @RequestParam(value = "startDate", required = true) startDate: Date,
            @ApiParam("endDate")
            @RequestParam(value = "endDate", required = true) endDate: Date,
    ) : List<Meet> {

        return meetInfoService.getMeetsBetweenDates(startDate, endDate)
    }

    @ApiOperation("Create a meet")
    @RequestMapping(value = ["xc/meets/create"], method = [RequestMethod.POST])
    fun createMeet(
        @RequestBody createMeetRequest: CreateMeetRequest
    ) : Meet {
        return meetInfoService.createMeet(createMeetRequest)
    }

    @ApiOperation("Update a meet")
    @RequestMapping(value = ["xc/meets/update"], method = [RequestMethod.PUT])
    fun updateMeet(
        @RequestBody createMeetRequest: CreateMeetRequest
    ) : Meet {
        return meetInfoService.updateMeet(createMeetRequest)
    }
}