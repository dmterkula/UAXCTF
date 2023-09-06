package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statistics.request.CreateMeetLogRequest
import com.terkula.uaxctf.statistics.service.MeetLogService
import com.terkula.uaxctf.training.response.MeetLogResponse

import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@Validated
class MeetLogController(val meetLogService: MeetLogService) {

    @ApiOperation("Gets the meet log entry for a runner at a given meet")
    @RequestMapping(value = ["xc/meet/log"], method = [RequestMethod.GET])
    fun getMeetLog(
            @ApiParam("MeetId")
            @RequestParam(value = "meetId", required = true) meetId: String,

            @ApiParam("RunnerId")
            @RequestParam(value = "runnerId", required = true) runnerId: Int

            ): MeetLogResponse {

        return meetLogService.getMeetLog(meetId, runnerId)
    }

    @ApiOperation("Gets the meet log entries for all runners at a given meet")
    @RequestMapping(value = ["xc/meet/logsAtMeet"], method = [RequestMethod.GET])
    fun getMeetLogsAtMeet(
            @ApiParam("MeetId")
            @RequestParam(value = "meetId", required = true) meetId: String,

    ): List<MeetLogResponse> {

        return meetLogService.getMeetLogsAtMeet(meetId)
    }

    @ApiOperation("Gets all meet logs for a runner in a season")
    @RequestMapping(value = ["xc/meet/logs/forRunnerInSeason"], method = [RequestMethod.GET])
    fun getMeetLogsForRunnerInSeason(
            @ApiParam("runnerId")
            @RequestParam(value = "runnerId", required = true) runnerId: Int,
            @ApiParam("season")
            @RequestParam(value = "season", required = true) season: String,

            ): List<MeetLogResponse> {

        return meetLogService.getAllMeetLogsForRunnerInSeason(runnerId, season).map { it.first }
    }

    @ApiOperation("Create Meet Log Entry")
    @RequestMapping(value = ["xc/meet/log/create"], method = [RequestMethod.POST])
    fun createMeetLog(@RequestBody createMeetLogRequest: CreateMeetLogRequest): MeetLogResponse {
        return meetLogService.createMeetLog(createMeetLogRequest)
    }

}