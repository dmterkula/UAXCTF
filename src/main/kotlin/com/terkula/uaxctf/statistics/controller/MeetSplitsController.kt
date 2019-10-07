package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statistics.response.RunnerMeetSplitResponse
import com.terkula.uaxctf.statistics.service.MeetMileSplitService
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.sql.Date

@RestController
class MeetSplitsController(@field:Autowired
                           internal var meetMileSplitService: MeetMileSplitService) {

    @ApiOperation("Returns Season Bests for all runners in the given season(s)")
    @RequestMapping(value = ["/xc/meetMileSplits/forRunner"], method = [RequestMethod.GET])
    fun getAllSeasonBestsByYear(
            @ApiParam("Filters results for runner matching the given name in the given season")
            @RequestParam(value = "filter.runner", required = true) name: String,
            @RequestParam(value = "filter.season", required = false, defaultValue = "") season: String): RunnerMeetSplitResponse {

        var startDate = Date.valueOf("${MeetPerformanceController.CURRENT_YEAR}-01-01")
        var endDate = Date.valueOf((MeetPerformanceController.CURRENT_YEAR) + "-12-31")


        if (season.isNotEmpty()) {
            startDate = Date.valueOf("$season-01-01")
            endDate = Date.valueOf("$season-12-31")
        }

        val mileSplitResponse = meetMileSplitService.getAllMeetMileSplitsForRunner(name, startDate, endDate)

        return mileSplitResponse

    }
}