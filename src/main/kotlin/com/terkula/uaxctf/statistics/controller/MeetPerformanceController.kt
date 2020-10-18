package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statistics.request.SortingMethodContainer
import com.terkula.uaxctf.statistics.response.HistoricalMeetComparisonResponse
import com.terkula.uaxctf.statistics.response.RunnerMeetPerformanceResponse
import com.terkula.uaxctf.statistics.service.MeetPerformanceService
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import java.sql.Date
import java.time.Year
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class MeetPerformanceController(@field:Autowired
                                internal var meetPerformanceService: MeetPerformanceService) {

    @ApiOperation("Returns the meet results for all runners matching the given name")
    @RequestMapping(value = ["xc/getMeetResultByName"], method = [RequestMethod.GET])
    fun getMeetResultsByName(
            @ApiParam("Get results for runners whose name contains this input. Can pass partial names to get results for multiple runners. " +
                    "e.g just passing common last name for sisters '")
            @RequestParam(value = "filter.runner") partialName: String,
            @ApiParam("Filters results for meets with a date after 01-01 of the input year")
            @RequestParam(value = "filter.startSeason", required = false, defaultValue = "") startSeason: String,
            @ApiParam("Filters results for meets within the start date and 12-31 of the input year")
            @RequestParam(value = "filter.endSeason", required = false, defaultValue = "") endSeason: String,
            @ApiParam("Sorts results based on the input valid sorting methods are 'date' and 'time'. " +
                    "If one these values is not provided, no sort is used ")
            @RequestParam(value = "sort.method", required = false, defaultValue = "time") sortMethod: String,
            @ApiParam("Limits the number of total results returned to the input")
            @RequestParam(value = "page.size", required = false, defaultValue = "10") count: Int,
            @ApiParam("Adjusts seasons bests for true distance of the meet if value passed is true")
            @RequestParam(value = "adjust.forDistance", required = false, defaultValue = "false") adjustForDistance: Boolean = false): RunnerMeetPerformanceResponse {

        var startDate = Date.valueOf("$CURRENT_YEAR-01-01")
        var endDate = Date.valueOf("$CURRENT_YEAR-12-31")

        if (startSeason.isNotEmpty()) {
            startDate = Date.valueOf("$startSeason-01-01")
        }

        if (endSeason.isNotEmpty()) {
            endDate = Date.valueOf("$endSeason-12-31")
        }

        val sortingMethodContainer = getSortingMethod(sortMethod)

        return RunnerMeetPerformanceResponse(meetPerformanceService.getMeetPerformancesForRunnerWithNameContaining(partialName,
                startDate, endDate, sortingMethodContainer, count, adjustForDistance))
    }

    @ApiOperation("Returns the meet results for the given runner in the given season")
    @RequestMapping(value = ["xc/getMeetResultByMeetName"], method = [RequestMethod.GET])
    fun getMeetResultsByMeetName(
            @ApiParam("Filters results based on runners whose name matches/partially matches the input name'")
            @RequestParam(value = "filter.meetName") partialName: String,
            @ApiParam("Filters results for meets with a date after 01-01 of the input year")
            @RequestParam(value = "filter.startSeason", required = false, defaultValue = "") startSeason: String,
            @ApiParam("Filters results for meets within the start date and 12-31 of the input year")
            @RequestParam(value = "filter.endSeason", required = false, defaultValue = "") endSeason: String,
            @ApiParam("Sorts results based on the input valid sorting methods are 'date' and 'time'. " +
                    "If one these values is not provided, no sort is used ")
            @RequestParam(value = "sort.method", required = false, defaultValue = "time") sortMethod: String,
            @ApiParam("Limits the number of total results returned to the input")
            @RequestParam(value = "page.size", required = false, defaultValue = "10") count: Int,
            @ApiParam("Adjusts seasons bests for true distance of the meet if value passed is true")
            @RequestParam(value = "adjust.forDistance", required = false, defaultValue = "false") adjustForDistance: Boolean = false): RunnerMeetPerformanceResponse {

        var startDate = Date.valueOf("$CURRENT_YEAR-01-01")
        var endDate = Date.valueOf("$CURRENT_YEAR-12-31")

        if (startSeason.isNotEmpty()) {
            startDate = Date.valueOf("$startSeason-01-01")
        }

        if (endSeason.isNotEmpty()) {
            endDate = Date.valueOf("$endSeason-12-31")
        }

        val sortingMethodContainer = getSortingMethod(sortMethod)

        return RunnerMeetPerformanceResponse(meetPerformanceService.getMeetPerformancesAtMeetName(partialName, startDate,
                endDate, sortingMethodContainer, count, adjustForDistance))
    }

    @ApiOperation("Returns the meet results for the given runner in the given season")
    @RequestMapping(value = ["xc/historicallyCompareMeets"], method = [RequestMethod.GET])
    fun historicallyCompareMeets(
            @ApiParam("name of the first meet you wish to look at")
            @RequestParam(value = "baseMeetName") baseMeetName: String,
            @ApiParam("name of meet you wish to compare performances too")
            @RequestParam(value = "compareMeetName") compareToMeetName: String,
            @ApiParam("excludes certain seasons from consideration of results")
            @RequestParam(value = "excludeSeasons", required = false, defaultValue = "") excludeSeasons: List<String>,
            @ApiParam("include only the given seasons when calculating the results")
            @RequestParam(value = "includeSeasons", required = false, defaultValue = "") includeSeasons: List<String>,
            @ApiParam("Adjusts seasons bests for true distance of the meet if value passed is true")
            @RequestParam(value = "adjust.forDistance", required = false, defaultValue = "false") adjustForDistance: Boolean = false): HistoricalMeetComparisonResponse {

        val startDate = Date.valueOf("$FIRST_YEAR_ON_RECORD-01-01")
        val endDate = Date.valueOf("$CURRENT_YEAR-12-31")


        return meetPerformanceService.compareTwoMeetsHistorically(baseMeetName, compareToMeetName, startDate, endDate,
                excludeSeasons, includeSeasons, adjustForDistance)
    }


    private fun getSortingMethod(sortMethod: String) =
            when (sortMethod) {
                "date" -> SortingMethodContainer.OLDER_DATE
                "time" -> SortingMethodContainer.TIME
                else -> SortingMethodContainer.NOSORT
            }

    companion object {

        val CURRENT_YEAR = Year.now().toString()
        val FIRST_YEAR_ON_RECORD = Year.of(2017).toString()
    }


}
