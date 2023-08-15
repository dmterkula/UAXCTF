package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statistics.request.CreateMeetLogRequest
import com.terkula.uaxctf.statistics.service.MeetLogService
import com.terkula.uaxctf.training.model.MeetLog

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
            @RequestParam(value = "meetId", required = true) meetId: Int,

            @ApiParam("RunnerId")
            @RequestParam(value = "runnerId", required = true) runnerId: Int

            ): MeetLog? {

        return meetLogService.getMeetLog(meetId, runnerId)
    }

    @ApiOperation("Create Meet Log Entry")
    @RequestMapping(value = ["xc/meet/log/create"], method = [RequestMethod.POST])
    fun createMeetLog(@RequestBody createMeetLogRequest: CreateMeetLogRequest): MeetLog {
        return meetLogService.createMeetLog(createMeetLogRequest)
    }

}