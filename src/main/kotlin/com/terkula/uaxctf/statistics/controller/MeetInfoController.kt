package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statisitcs.model.Meet
import com.terkula.uaxctf.statistics.request.CreateMeetRequest
import com.terkula.uaxctf.statistics.response.MeetResponse
import com.terkula.uaxctf.statistics.response.track.TrackAndXcMeetResponse
import com.terkula.uaxctf.statistics.service.MeetInfoService
import org.springframework.web.bind.annotation.*

import java.sql.Date

@RestController
class MeetInfoController(var meetInfoService: MeetInfoService) {

    @RequestMapping(value = ["xc/getMeetNames"], method = [RequestMethod.GET])
    fun getMeets(
            @RequestParam(value = "team", required = false, defaultValue = "UA") team: String? = "UA"
    ) : MeetResponse {

        var teamString = team ?: "UA"
       return MeetResponse(meetInfoService.getMeetInfo(teamString))
    }

    @RequestMapping(value = ["/getAllMeetNames"], method = [RequestMethod.GET])
    fun getAllMeets(
            @RequestParam(value = "team", required = false, defaultValue = "UA") team: String? = "UA"
    ) : TrackAndXcMeetResponse {

        var teamString = team ?: "UA"
        return TrackAndXcMeetResponse(meetInfoService.getMeetInfo(teamString), meetInfoService.getTrackMeetInfo(teamString))
    }

    @RequestMapping(value = ["xc/meets/getBySeason"], method = [RequestMethod.GET])
    fun getMeetsForSeason(
        @RequestParam(value = "season", required = true) season: String,
    ) : List<Meet> {

        return meetInfoService.getMeetsForSeason(season)
    }

    @RequestMapping(value = ["xc/meets/getByDates"], method = [RequestMethod.GET])
    fun getMeetsBetweenDates(
            @RequestParam(value = "startDate", required = true) startDate: Date,
            @RequestParam(value = "endDate", required = true) endDate: Date,
    ) : List<Meet> {

        return meetInfoService.getMeetsBetweenDates(startDate, endDate)
    }

    @RequestMapping(value = ["xc/meets/create"], method = [RequestMethod.POST])
    fun createMeet(
        @RequestBody createMeetRequest: CreateMeetRequest
    ) : Meet {
        return meetInfoService.createMeet(createMeetRequest)
    }

    @RequestMapping(value = ["xc/meets/update"], method = [RequestMethod.PUT])
    fun updateMeet(
        @RequestBody createMeetRequest: CreateMeetRequest
    ) : Meet {
        return meetInfoService.updateMeet(createMeetRequest)
    }
}