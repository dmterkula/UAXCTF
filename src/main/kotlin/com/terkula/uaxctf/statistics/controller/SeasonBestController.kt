package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statistics.response.SeasonBestResponse
import com.terkula.uaxctf.statistics.response.TTestResponse
import com.terkula.uaxctf.statistics.service.SeasonBestService
import com.terkula.uaxctf.statistics.service.TimeTrialService
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import java.sql.Date
import java.util.stream.IntStream
import kotlin.streams.toList

@RestController
class SeasonBestController(@field:Autowired
                   internal var seasonBestService: SeasonBestService,
                   var timeTrialService: TimeTrialService) {


    @ApiOperation("Returns Season Bests for all runners in the given season(s)")
    @RequestMapping(value = ["/xc/seasonBests/all"], method = [RequestMethod.GET])
    fun getAllSeasonBestsByYear(
    @ApiParam("Filters results for meets with a date after 01-01 of the input year")
    @RequestParam(value = "filter.season", required = false, defaultValue = "") season: String,
    @ApiParam("Adjusts seasons bests for true distance of the meet if value passed is true")
    @RequestParam(value = "adjust.forDistance", required = false, defaultValue = "false") adjustForDistance: Boolean = false
    ) : SeasonBestResponse {

        var startDate = Date.valueOf("${MeetPerformanceController.CURRENT_YEAR}-01-01")
        var endDate = Date.valueOf((MeetPerformanceController.CURRENT_YEAR) + "-12-31")


        if (season.isNotEmpty()) {
            startDate = Date.valueOf("$season-01-01")
            endDate = Date.valueOf("$season-12-31")
        }

        val seasonBests = seasonBestService.getAllSeasonBests(startDate, endDate, adjustForDistance)

        return SeasonBestResponse(seasonBests.size, seasonBests)

    }

    @ApiOperation("Returns a singular runner's Season Bests within the last number of given years")
    @RequestMapping(value = ["xc/seasonBests/"], method = [RequestMethod.GET])
    fun getRunnerSeasonBestsByYear(
            @ApiParam("Limits results for the last number of years based on the input")
            @RequestParam(value = "limit.numYears", required = false, defaultValue = "1") numSeasons: Int,
            @ApiParam("Filters results athletes whose name matches/partially matches the input name")
            @RequestParam(value = "filter.name") partialName: String,
            @ApiParam("Adjusts seasons bests for true distance of the meet if value passed is true")
            @RequestParam(value = "adjust.forDistance", required = false, defaultValue = "false") adjustForDistance: Boolean = false) : SeasonBestResponse {

        val startEndPairs = IntStream.range(1, numSeasons + 1).toList().map {
            val startSeasonYear = MeetPerformanceController.CURRENT_YEAR.toInt() - (it - 1)
            val startDate = Date.valueOf("$startSeasonYear-01-01")
            val endDate = Date.valueOf("$startSeasonYear-12-31")
            startDate to endDate
        }

        val seasonBests = seasonBestService.getSeasonBestsByName(partialName, startEndPairs, adjustForDistance)

        return SeasonBestResponse(seasonBests.size, seasonBests)

    }

    @RequestMapping(value = ["xc/seasonBests/lastMeet"], method = [RequestMethod.GET])
    fun getSeasonBestsAtLastMeet(
            @ApiParam("Adjusts seasons bests for true distance of the meet if value passed is true")
            @RequestParam(value = "adjust.forDistance", required = false, defaultValue = "false") adjustForDistance: Boolean = false
    ): SeasonBestResponse {
        val startDate = Date.valueOf("${MeetPerformanceController.CURRENT_YEAR}-01-01")
        val endDate = Date.valueOf((MeetPerformanceController.CURRENT_YEAR) + "-12-31")

        val seasonBests = seasonBestService.getSeasonBestsAtLastMeet(startDate, endDate, adjustForDistance)

        return SeasonBestResponse(seasonBests.size, seasonBests)
    }

    @ApiOperation("Returns runner's season best if their current season best is still the first meet")
    @RequestMapping(value = ["xc/seasonBests/isAtFirstMeet"], method = [RequestMethod.GET])
    fun findWhoseSeasonBestIsFirstMeet(
            @ApiParam("Adjusts seasons bests for true distance of the meet if value passed is true")
            @RequestParam(value = "adjust.forDistance", required = false, defaultValue = "false") adjustForDistance: Boolean = false
    ): SeasonBestResponse {

        val startDate = Date.valueOf("${MeetPerformanceController.CURRENT_YEAR}-01-01")
        val endDate = Date.valueOf((MeetPerformanceController.CURRENT_YEAR) + "-12-31")


        val seasonBests = seasonBestService.findWhoseSeasonBestIsFirstMeet(startDate, endDate, adjustForDistance)

        return SeasonBestResponse(seasonBests.size, seasonBests)

    }

    @ApiOperation("Returns T Test between two difference in runners SBs times between consecutive years. Intent is to use as indicator training of a given season")
    @RequestMapping(value = ["xc/seasonBests/yearToYearTTest"], method = [RequestMethod.GET])
    fun runTTestBetweenSBAndPreviousSB( @ApiParam("use time trial and SB data from this season")
                                                    @RequestParam("filter.baseSeason") baseSeason: String,
                                                    @RequestParam("filter.comparisonSeason") comparisonSeason: String,
                                                    @RequestParam(value = "adjustForMeetDistance", required = false, defaultValue = "false") adjustForMeetDistance: Boolean = false
    ): TTestResponse {

        val startDate1 = Date.valueOf("$baseSeason-01-01")
        val endDate1 = Date.valueOf("$baseSeason-12-31")

        val startDate2 = Date.valueOf("$comparisonSeason-01-01")
        val endDate2 = Date.valueOf("$comparisonSeason-12-31")

        return timeTrialService.runTTestBetweenSBAndPreviusYearSB(startDate1, endDate1, startDate2, endDate2, adjustForMeetDistance)
    }

}
