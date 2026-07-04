package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statistics.dto.StatisticalComparisonDTO
import com.terkula.uaxctf.statistics.dto.TimeTrialDTO
import com.terkula.uaxctf.statistics.dto.TimeTrialDifferenceDTO
import com.terkula.uaxctf.statistics.response.TTestResponse
import com.terkula.uaxctf.statistics.response.TimeTrialProgressionResponse
import com.terkula.uaxctf.statistics.service.TimeTrialService
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
class TimeTrialController (@field:Autowired
                                      internal val timeTrialService: TimeTrialService) {
    @RequestMapping(value = ["xc/timeTrial/progression"], method = [RequestMethod.GET])
    fun getTimeTrialProgressions(
                                   @RequestParam("filter.season", defaultValue = "") season: String,
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

        return TimeTrialProgressionResponse(timeTrialService.getRankedProgressionSinceTimeTrial(startDate, endDate, adjustForMeetDistance))
    }

    @RequestMapping(value = ["xc/timeTrialResults"], method = [RequestMethod.GET])
    fun getTimeTrialResults(
            @RequestParam("filter.season", defaultValue = "") season: String,
            @RequestParam("filter.scaleTo5k", defaultValue = "true") scaleTo5k: Boolean
    ) : List<TimeTrialDTO> {

        var startDate = Date.valueOf("${MeetPerformanceController.CURRENT_YEAR}-01-01")
        var endDate = Date.valueOf((MeetPerformanceController.CURRENT_YEAR) + "-12-31")


        if (season.isNotEmpty()) {
            startDate = Date.valueOf("$season-01-01")
            endDate = Date.valueOf("$season-12-31")
        }

        return timeTrialService.getTimeTrialResults(startDate, endDate, scaleTo5k)
    }

    @RequestMapping(value = ["xc/timeTrialResults/compareRunnersBetweenYears"], method = [RequestMethod.GET])
    fun getTimeTrialComparisionsBetweenInputYearAndPreviousYearForReturningRunners(
            @RequestParam("filter.season", defaultValue = "") season: String
    ) : List<TimeTrialDifferenceDTO> {

        var startDate = Date.valueOf("${MeetPerformanceController.CURRENT_YEAR}-01-01")
        var endDate = Date.valueOf((MeetPerformanceController.CURRENT_YEAR) + "-12-31")


        if (season.isNotEmpty()) {
            startDate = Date.valueOf("$season-01-01")
            endDate = Date.valueOf("$season-12-31")
        }

        return timeTrialService.getTimeTrialComparisonsBetweenYearsForSameRunners(startDate, endDate)
    }

    @RequestMapping(value = ["xc/timeTrial/seasonBestToTimeTrial"], method = [RequestMethod.GET])
    fun getDifferenceBetweenPreviousSeasonBestAndTimeTrial(
                                                            @RequestParam("filter.seasons") seasons: List<String>,
                                                            @RequestParam(value = "adjustForMeetDistance", required = false, defaultValue = "false") adjustForMeetDistance: Boolean = false
    ): List<StatisticalComparisonDTO> {

        return seasons.map {
            val startDate = Date.valueOf("$it-01-01")
            val endDate = Date.valueOf("$it-12-31")
            timeTrialService.getPreviousSBsToTimeTrialDifference(startDate, endDate, adjustForMeetDistance)
        }
    }

    @RequestMapping(value = ["xc/timeTrial/summerConditioningTTest"], method = [RequestMethod.GET])
    fun runTTestBetweenPreviousSeasonBestsAndTimeTrialTimes(
                                                            @RequestParam("filter.baseSeason") baseSeason: String,
                                                            @RequestParam("filter.comparisonSeason") comparisonSeason: String,
                                                            @RequestParam(value = "adjustForMeetDistance", required = false, defaultValue = "false") adjustForMeetDistance: Boolean = false
    ): TTestResponse {

        val startDate1 = Date.valueOf("$baseSeason-01-01")
        val endDate1 = Date.valueOf("$baseSeason-12-31")

        val startDate2 = Date.valueOf("$comparisonSeason-01-01")
        val endDate2 = Date.valueOf("$comparisonSeason-12-31")

        return timeTrialService.runTTestBetweenPreviousSBsToTimeTrial(startDate1, endDate1, startDate2, endDate2, adjustForMeetDistance)
    }

    @RequestMapping(value = ["xc/timeTrial/yearToYearImprovementTTest"], method = [RequestMethod.GET])
    fun runTTestBetweenTimeTrialImprovementsYearToYear(
                                                             @RequestParam("filter.baseSeason") baseSeason: String,
                                                             @RequestParam("filter.comparisonSeason") comparisonSeason: String,
                                                             @RequestParam(value = "adjustForMeetDistance", required = false, defaultValue = "false") adjustForMeetDistance: Boolean = false
    ): TTestResponse {

        val startDate1 = Date.valueOf("$baseSeason-01-01")
        val endDate1 = Date.valueOf("$baseSeason-12-31")

        val startDate2 = Date.valueOf("$comparisonSeason-01-01")
        val endDate2 = Date.valueOf("$comparisonSeason-12-31")

        return timeTrialService.runTTestBetweenTimeTrialDifferencesForReturningRunners(startDate1, endDate1, startDate2, endDate2, adjustForMeetDistance)
    }

    @RequestMapping(value = ["xc/timeTrial/seasonImprovementTTest"], method = [RequestMethod.GET])
    fun runTTestBetweenTimeTrialAndEndOfSeasonBest(
                                                             @RequestParam("filter.baseSeason") baseSeason: String,
                                                             @RequestParam("filter.comparisonSeason") comparisonSeason: String,
                                                             @RequestParam(value = "adjustForMeetDistance", required = false, defaultValue = "false") adjustForMeetDistance: Boolean = false
    ): TTestResponse {

        val startDate1 = Date.valueOf("$baseSeason-01-01")
        val endDate1 = Date.valueOf("$baseSeason-12-31")

        val startDate2 = Date.valueOf("$comparisonSeason-01-01")
        val endDate2 = Date.valueOf("$comparisonSeason-12-31")

        return timeTrialService.runTTestBetweenTimeTrialAndSB(startDate1, endDate1, startDate2, endDate2, adjustForMeetDistance)
    }


    @RequestMapping(value = ["xc/timeTrial/result/create"], method = [RequestMethod.POST])
    fun createTimeTrialResult(
                                  @RequestParam("season", defaultValue = "") season: String,
                               @RequestParam("runnerId") runnerId: Int,
                               @RequestParam("time") time: String,
                               @RequestParam("place") place: Int
    ): TimeTrialDTO {
        return timeTrialService.createTimeTrialResult(season, runnerId, time, place)
    }
}