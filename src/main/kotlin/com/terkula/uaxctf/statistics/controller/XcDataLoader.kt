package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statistics.repository.MeetRepository
import com.terkula.uaxctf.statistics.service.MeetPerformanceService
import io.swagger.annotations.ApiOperation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

@RestController
class XcDataLoader(@field:Autowired
                   internal var meetPerformanceService: MeetPerformanceService) {

    @ApiOperation("Loads meet data that has manually been entered in the load table.")
    @RequestMapping(value = ["xc/load/{meetId}"], method = [RequestMethod.GET])
    fun loadPerformances(@PathVariable("meetId") meetId: Int): String {

        meetPerformanceService.loadMeetPerformance(meetId)
        return "loaded"
    }


}
