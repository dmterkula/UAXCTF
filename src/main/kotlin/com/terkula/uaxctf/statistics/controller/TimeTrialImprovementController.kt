package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statistics.dto.StatisticalComparisonDTO
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
    fun getTimeTrialProgressions( @ApiParam("filters results for performances in a given season")
                                   @RequestParam("filter.season", defaultValue = "") season: String,
                                   @ApiParam("The value provided sort.method changes what order the results are returned in. " +
                                           "Valid values are 'least', 'most'. Use most to see who progressed the most")
                                   @Pattern(
                                           regexp = "least|most|",
                                           message = "Valid values are 'least' to return" +
                                                   " runners with the smallest progression,  or 'most' to return those with the largest progression")
                                   @RequestParam(value = "sort.method", required = false, defaultValue = "most") sort: String = "most",
                                   @RequestParam(value = "adjustForMeetDistance", required = false, defaultValue = "false") adjustForMeetDistance: Boolean = false): TimeTrialProgressionResponse {


        var startDate = Date.valueOf("${MeetPerformanceController.CURRENT_YEAR}-01-01")
        var endDate = Date.valueOf((MeetPerformanceController.CURRENT_YEAR) + "-12-31")


        if (season.isNotEmpty()) {
            startDate = Date.valueOf("$season-01-01")
            endDate = Date.valueOf("$season-12-31")
        }

        return TimeTrialProgressionResponse(timeTrialProgressionService.getRankedProgressionSinceTimeTrial(startDate, endDate, adjustForMeetDistance))
    }

    @ApiOperation("Returns statistical comparision between a given year's time trial, and last years SBs. Intent is to use as indicator for relative summer conditioning levels. " +
            "can be used to get data for multiple years, or just one")
    @RequestMapping(value = ["xc/timeTrial/seasonBestToTimeTrial"], method = [RequestMethod.GET])
    fun getDifferenceBetweenPreviousSeasonBestAndTimeTrial( @ApiParam("use time trial data from this season and SBs from season prior")
                                                            @RequestParam("filter.seasons") seasons: List<String>,
                                                            @RequestParam(value = "adjustForMeetDistance", required = false, defaultValue = "false") adjustForMeetDistance: Boolean = false
    ): List<StatisticalComparisonDTO> {

        return seasons.map {
            val startDate = Date.valueOf("$it-01-01")
            val endDate = Date.valueOf("$it-12-31")
            timeTrialProgressionService.getPreviousSBsToTimeTrialDifference(startDate, endDate, adjustForMeetDistance)
        }
    }

}