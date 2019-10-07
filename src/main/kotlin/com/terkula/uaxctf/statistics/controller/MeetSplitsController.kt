package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statistics.response.RunnerAvgSplitDifferenceResponse
import com.terkula.uaxctf.statistics.response.RunnerMeetSplitResponse
import com.terkula.uaxctf.statistics.service.MeetMileSplitService
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
class MeetSplitsController(@field:Autowired
                           internal var meetMileSplitService: MeetMileSplitService) {

    @ApiOperation("Returns the splits per mile for all runners in the given season(s)")
    @RequestMapping(value = ["/xc/meetMileSplits/forRunner"], method = [RequestMethod.GET])
    fun getAllMileSplitsBySeasonForRunner(
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

    @ApiOperation("Returns runners who on average slowed down the most or sped up between certain miles in races")
    @RequestMapping(value = ["/xc/avgMeetMileSplits/"], method = [RequestMethod.GET])
    fun getAverageSlowDownPerMile(
            @ApiParam("Filters results the given meet, averages for all meets there is data for if none provided")
            @RequestParam(value = "filter.meet", required = false, defaultValue = "") filterMeet: String = "",
            @ApiParam("Filters results for runner matching the given name in the given season")
            @RequestParam(value = "filter.season", required = false, defaultValue = "") season: String,
            @ApiParam("Filters results for runner matching the given name in the given season." +
                    "Valid values are 'firstTwoSecond', 'secondToThird' or 'all'")
            @Pattern(
                    regexp = "firstToSecond|secondToThird|all|spread",
                    message = "The value provided for filter.split changes what split differences are calculated. Valid values are spread, 'firstTwoSecond', 'secondToThird' or 'all'")
            @RequestParam(value = "filter.split", required = false, defaultValue = "all") split: String = "all",
            @ApiParam("The value provided sort.method changes what order the results are returned in." +
                    "The value provided sort.method changes what order the results are returned in. Valid values are 'lowest', 'highest'")
            @Pattern(
                    regexp = "lowest|highest|",
                    message = "Valid values are 'lowest' to return" +
                            " runners with lower split differences,  or 'higher' to return those with the highest differences")
            @RequestParam(value = "sort.method", required = false, defaultValue = "lowest") sort: String = "lowest",
            @ApiParam("Limits the number of results to the desired counts")
            @RequestParam(value = "page.size", required = false, defaultValue = "50") limit: Int = 50): RunnerAvgSplitDifferenceResponse {

        var startDate = Date.valueOf("${MeetPerformanceController.CURRENT_YEAR}-01-01")
        var endDate = Date.valueOf((MeetPerformanceController.CURRENT_YEAR) + "-12-31")

        if (season.isNotEmpty()) {
            startDate = Date.valueOf("$season-01-01")
            endDate = Date.valueOf("$season-12-31")
        }

       return RunnerAvgSplitDifferenceResponse ( meetMileSplitService.getMeetSplitInfo(filterMeet, split, startDate, endDate, sort, limit))

    }

}