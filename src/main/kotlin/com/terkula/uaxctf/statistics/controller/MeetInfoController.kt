package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statisitcs.model.Meet
import com.terkula.uaxctf.statistics.response.MeetResponse
import com.terkula.uaxctf.statistics.service.MeetInfoService
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.sql.Date
import java.util.*

@RestController
class MeetInfoController(var meetInfoService: MeetInfoService) {

    @ApiOperation("Returns a List of meet names to select for results ")
    @RequestMapping(value = ["xc/getMeetNames"], method = [RequestMethod.GET])
    fun getMeets() : MeetResponse {

       return MeetResponse(meetInfoService.getMeetInfo())
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
}