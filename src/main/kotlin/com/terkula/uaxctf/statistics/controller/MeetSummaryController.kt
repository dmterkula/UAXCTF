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

    @RequestMapping(value = ["/meetSummary"], method = [RequestMethod.GET])
    fun getLastMeetSummary(@RequestParam(value = "filter.meet", required = true) meetName: String): MeetSummaryResponse {

        var startDate = Date.valueOf("${MeetPerformanceController.CURRENT_YEAR}-01-01")
        var endDate = Date.valueOf((MeetPerformanceController.CURRENT_YEAR) + "-12-31")

        return meetSummaryService.getLastMeetSummary(startDate, endDate, meetName)

    }


}