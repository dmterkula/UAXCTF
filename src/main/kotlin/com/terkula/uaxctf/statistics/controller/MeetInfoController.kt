package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statistics.response.MeetResponse
import com.terkula.uaxctf.statistics.service.MeetInfoService
import io.swagger.annotations.ApiOperation

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

@RestController
class MeetInfoController(var meetInfoService: MeetInfoService) {

    @ApiOperation("Returns a List of meet names to select for results ")
    @RequestMapping(value = ["xc/getMeetNames"], method = [RequestMethod.GET])
    fun getMeets() : MeetResponse {

       return MeetResponse(meetInfoService.getMeetInfo())
    }


}