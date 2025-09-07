package com.terkula.uaxctf.statistics.controller.track

import com.terkula.uaxctf.statisitcs.model.Meet
import com.terkula.uaxctf.statisitcs.model.track.TrackMeet
import com.terkula.uaxctf.statistics.request.CreateMeetRequest
import com.terkula.uaxctf.statistics.response.MeetResponse
import com.terkula.uaxctf.statistics.response.track.TrackMeetSummaryResponse
import com.terkula.uaxctf.statistics.service.MeetInfoService
import com.terkula.uaxctf.statistics.service.track.TrackMeetPerformanceService
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.web.bind.annotation.*
import java.sql.Date

@RestController
class TrackMeetController(
    var meetInfoService: MeetInfoService,
    var trackMeetPerformanceService: TrackMeetPerformanceService
    ) {

    @ApiOperation("Create a meet")
    @RequestMapping(value = ["track/meets/create"], method = [RequestMethod.POST])
    fun createMeet(
            @RequestBody createMeetRequest: CreateMeetRequest
    ) : TrackMeet {
        return meetInfoService.createTrackMeet(createMeetRequest)
    }

    @ApiOperation("Update a meet")
    @RequestMapping(value = ["track/meets/update"], method = [RequestMethod.PUT])
    fun updateMeet(
            @RequestBody createMeetRequest: CreateMeetRequest
    ) : TrackMeet {
        return meetInfoService.updateTrackMeet(createMeetRequest)
    }

    @ApiOperation("Returns a List of meet names to select for results ")
    @RequestMapping(value = ["track/meets/getByDates"], method = [RequestMethod.GET])
    fun getMeetsBetweenDates(
            @ApiParam("startDate")
            @RequestParam(value = "startDate", required = true) startDate: Date,
            @ApiParam("endDate")
            @RequestParam(value = "endDate", required = true) endDate: Date,
    ) : List<TrackMeet> {

        return meetInfoService.getTrackMeetsBetweenDates(startDate, endDate)
    }
    @ApiOperation("Returns a List of meet names to select for results ")
    @RequestMapping(value = ["track/meets/getBySeason"], method = [RequestMethod.GET])
    fun getMeetsForSeason(
            @ApiParam("season")
            @RequestParam(value = "season", required = true) season: String,
    ) : List<Meet> {

        return meetInfoService.getMeetsForSeason(season)
    }

    @ApiOperation("Returns a List of meet names to select for results ")
    @RequestMapping(value = ["track/getMeetNames"], method = [RequestMethod.GET])
    fun getMeets(
            @RequestParam(value = "team", required = false, defaultValue = "UA") team: String? = "UA"
    ) : MeetResponse {

        var teamString = team ?: "UA"
        return MeetResponse(meetInfoService.getTrackMeetInfo(teamString))
    }

    @ApiOperation("Returns Meet Summary For Track Meet ")
    @RequestMapping(value = ["track/getMeetSummary"], method = [RequestMethod.GET])
    fun getMeetSummary(
        @ApiParam("meetUUID")
        @RequestParam(value = "meetUUID", required = true) meetUUID: String,
        @ApiParam("includeSplits")
        @RequestParam(value = "includeSplits", required = false) includeSplits: Boolean? = false
    ): TrackMeetSummaryResponse {

        var useSplits = false;
        if (includeSplits != null) {
            useSplits = includeSplits
        }

        return trackMeetPerformanceService.getTrackMeetSummary(meetUUID, useSplits)
    }

    @ApiOperation("Returns Meet Summary For Track Meet ")
    @RequestMapping(value = ["track/getMeetSummaryByNameAndDate"], method = [RequestMethod.GET])
    fun getMeetSummaryByNameAndSeason(
            @ApiParam("meetName")
            @RequestParam(value = "meetName", required = true) meetName: String,
            @ApiParam("season")
            @RequestParam(value = "season", required = true) season: String,
            @ApiParam("includeSplits")
            @RequestParam(value = "includeSplits", required = false) includeSplits: Boolean? = false
    ): TrackMeetSummaryResponse {

        var useSplits = false;
        if (includeSplits != null) {
            useSplits = includeSplits
        }

        return trackMeetPerformanceService.getTrackMeetSummaryForMeetNameAndSeason(meetName, season, useSplits)
    }

}