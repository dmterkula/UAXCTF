package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statistics.response.MeetSummaryResponse
import com.terkula.uaxctf.statistics.service.MeetSummaryService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.sql.Date

@RestController
class MeetSummaryController(@field:Autowired
                             internal var meetSummaryService: MeetSummaryService) {

    @RequestMapping(value = ["xc/meetSummary"], method = [RequestMethod.GET])
    fun getLastMeetSummary(
            @RequestParam(value = "filter.meet", required = true) meetName: String,
            @RequestParam(value = "page.size", required = false, defaultValue = "7") limit: Int,
            @RequestParam(value = "adjust.forDistance", required = false, defaultValue = "false")
            adjustForDistance: Boolean = false,
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