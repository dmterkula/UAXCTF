package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statistics.response.SeasonBestResponse
import com.terkula.uaxctf.statistics.response.TTestResponse
import com.terkula.uaxctf.statistics.service.SeasonBestService
import com.terkula.uaxctf.statistics.service.TimeTrialService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import java.sql.Date
import java.util.stream.IntStream
import kotlin.streams.toList

@RestController
class SeasonBestController(@field:Autowired
                   internal var seasonBestService: SeasonBestService,
                   var timeTrialService: TimeTrialService) {


    @RequestMapping(value = ["/xc/seasonBests/all"], method = [RequestMethod.GET])
    fun getAllSeasonBestsByYear(
    @RequestParam(value = "filter.season", required = false, defaultValue = "") season: String,
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

    @RequestMapping(value = ["xc/seasonBests/"], method = [RequestMethod.GET])
    fun getRunnerSeasonBestsByYear(
            @RequestParam(value = "limit.numYears", required = false, defaultValue = "1") numSeasons: Int,
            @RequestParam(value = "filter.name") partialName: String,
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
            @RequestParam(value = "adjust.forDistance", required = false, defaultValue = "false") adjustForDistance: Boolean = false
    ): SeasonBestResponse {
        val startDate = Date.valueOf("${MeetPerformanceController.CURRENT_YEAR}-01-01")
        val endDate = Date.valueOf((MeetPerformanceController.CURRENT_YEAR) + "-12-31")

        val seasonBests = seasonBestService.getSeasonBestsAtLastMeet(startDate, endDate, adjustForDistance)

        return SeasonBestResponse(seasonBests.size, seasonBests)
    }

    @RequestMapping(value = ["xc/seasonBests/isAtFirstMeet"], method = [RequestMethod.GET])
    fun findWhoseSeasonBestIsFirstMeet(
            @RequestParam(value = "adjust.forDistance", required = false, defaultValue = "false") adjustForDistance: Boolean = false
    ): SeasonBestResponse {

        val startDate = Date.valueOf("${MeetPerformanceController.CURRENT_YEAR}-01-01")
        val endDate = Date.valueOf((MeetPerformanceController.CURRENT_YEAR) + "-12-31")


        val seasonBests = seasonBestService.findWhoseSeasonBestIsFirstMeet(startDate, endDate, adjustForDistance)

        return SeasonBestResponse(seasonBests.size, seasonBests)

    }

    @RequestMapping(value = ["xc/seasonBests/yearToYearTTest"], method = [RequestMethod.GET])
    fun runTTestBetweenSBAndPreviousSB(
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
