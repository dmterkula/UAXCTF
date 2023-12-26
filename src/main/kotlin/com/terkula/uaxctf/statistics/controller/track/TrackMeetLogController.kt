package com.terkula.uaxctf.statistics.controller.track

import com.terkula.uaxctf.statistics.request.track.CreateTrackMeetLogRequest
import com.terkula.uaxctf.statistics.service.MeetLogService
import com.terkula.uaxctf.training.response.track.TrackMeetLogResponse
import com.terkula.uaxctf.util.TimeUtilities
import com.terkula.uaxctf.util.subtractDays
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@Validated
class TrackMeetLogController(val meetLogService: MeetLogService) {

    @ApiOperation("Gets the meet log entry for a runner at a given meet")
    @RequestMapping(value = ["track/meetLog"], method = [RequestMethod.GET])
    fun getTrackMeetLogs(
            @ApiParam("MeetId")
            @RequestParam(value = "meetId", required = true) meetId: String,

            @ApiParam("RunnerId")
            @RequestParam(value = "runnerId", required = true) runnerId: Int

    ): TrackMeetLogResponse {

        return meetLogService.getTrackMeetLogs(meetId, runnerId)
    }

    @ApiOperation("Gets the meet log entries for all runners at a given meet")
    @RequestMapping(value = ["track/meetLogs/AtMeet"], method = [RequestMethod.GET])
    fun getMeetLogsAtMeet(
            @ApiParam("MeetId")
            @RequestParam(value = "meetId", required = true) meetId: String,

            ): List<TrackMeetLogResponse> {

        return meetLogService.getTrackMeetLogsAtMeet(meetId)
    }

    @ApiOperation("Gets all meet logs for a runner in a season")
    @RequestMapping(value = ["track/meetLogs/forRunnerInSeason"], method = [RequestMethod.GET])
    fun getMeetLogsForRunnerInSeason(
            @ApiParam("runnerId")
            @RequestParam(value = "runnerId", required = true) runnerId: Int,
            @ApiParam("season")
            @RequestParam(value = "season", required = true) season: String,
            @RequestParam(value = "type", required = false) type: String?

    ): List<TrackMeetLogResponse> {

        var xcOrTrack = "xc"
        if (type != null) {
            xcOrTrack = type
        }

        var startDate = TimeUtilities.getFirstDayOfGivenYear(season)
        var endDate = TimeUtilities.getLastDayOfGivenYear(season)

        if (xcOrTrack.equals("track", ignoreCase = true)) {
            startDate = startDate.subtractDays(90)
            endDate = TimeUtilities.getLastDayOfGivenYear(season).subtractDays(150)
        }

        return meetLogService.getAllTrackMeetLogsForRunnerInSeason(runnerId, startDate, endDate, xcOrTrack).map { it.first }
    }

    @ApiOperation("Create Meet Log Entry")
    @RequestMapping(value = ["track/meetLogs/create"], method = [RequestMethod.POST])
    fun createTrackMeetLogs(@RequestBody createMeetLogRequest: CreateTrackMeetLogRequest): TrackMeetLogResponse {
        return meetLogService.createTrackMeetLogs(createMeetLogRequest)
    }

}