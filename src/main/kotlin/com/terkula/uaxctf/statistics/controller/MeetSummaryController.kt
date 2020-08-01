package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statistics.response.MeetSummaryResponse
import com.terkula.uaxctf.statistics.service.MeetSummaryService
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.sql.Date

@RestController
class MeetSummaryController(@field:Autowired
                             internal var meetSummaryService: MeetSummaryService) {

    @ApiOperation("Returns a summary of the last meet matching the given name. " +
            "Summary includes Season Bests, PRs, Comparison to meets this year and the same meet last year")
    @RequestMapping(value = ["xc/meetSummary"], method = [RequestMethod.GET])
    fun getLastMeetSummary(
            @ApiParam("Filters results for just a meet matching the given meet name/partial name")
            @RequestParam(value = "filter.meet", required = true) meetName: String,
            @ApiParam("Limits number of results for improvement rates and fastest lastMile")
            @RequestParam(value = "page.size", required = false, defaultValue = "7") limit: Int,
            @ApiParam("Adjusts seasons bests for true distance of the meet if value passed is true")
            @RequestParam(value = "adjust.forDistance", required = false, defaultValue = "false")
            adjustForDistance: Boolean = false,
            @ApiParam("Specifies the season for the summary. defaults to current year")
            @RequestParam(value = "season", required = false, defaultValue = "")
            season: String = ""): MeetSummaryResponse {


        var startDate = Date.valueOf("${MeetPerformanceController.CURRENT_YEAR}-01-01")
        var endDate = Date.valueOf((MeetPerformanceController.CURRENT_YEAR) + "-12-31")

        if (season.isNotEmpty()) {
            startDate = Date.valueOf("$season-01-01")
            endDate = Date.valueOf("$season-12-31")
        }

        return meetSummaryService.getLastMeetSummary(startDate, endDate, meetName, limit, adjustForDistance)

    }

}