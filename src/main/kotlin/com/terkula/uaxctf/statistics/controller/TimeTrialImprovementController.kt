package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statistics.response.TimeTrialProgressionResponse
import com.terkula.uaxctf.statistics.service.TimeTrialProgressionService
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.sql.Date
import javax.validation.constraints.Pattern

@RestController
@Validated
class TimeTrialImprovementController (@field:Autowired
                                      internal val timeTrialProgressionService: TimeTrialProgressionService) {
    @ApiOperation("Returns information on who improved the most on their 5k adjusted time trial times")
    @RequestMapping(value = ["xc/timeTrial/progression"], method = [RequestMethod.GET])
    fun gertTimeTrialProgressions( @ApiParam("filters results for performances in a given season")
                                   @RequestParam("filter.season", defaultValue = "") season: String,
                                   @ApiParam("The value provided sort.method changes what order the results are returned in. " +
                                           "Valid values are 'least', 'most'. Use most to see who progressed the most")
                                   @Pattern(
                                           regexp = "least|most|",
                                           message = "Valid values are 'least' to return" +
                                                   " runners with the smallest progression,  or 'most' to return those with the largest progression")
                                   @RequestParam(value = "sort.method", required = false, defaultValue = "most") sort: String = "most"): TimeTrialProgressionResponse {


        var startDate = Date.valueOf("${MeetPerformanceController.CURRENT_YEAR}-01-01")
        var endDate = Date.valueOf((MeetPerformanceController.CURRENT_YEAR) + "-12-31")


        if (season.isNotEmpty()) {
            startDate = Date.valueOf("$season-01-01")
            endDate = Date.valueOf("$season-12-31")
        }

        return TimeTrialProgressionResponse(timeTrialProgressionService.getRankedProgressionSinceTimeTrial(startDate, endDate))




    }


}