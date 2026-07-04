package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statistics.dto.RunnerMeetPerformanceDTO
import com.terkula.uaxctf.statistics.request.SortingMethodContainer
import com.terkula.uaxctf.statistics.dto.StatisticalComparisonDTO
import com.terkula.uaxctf.statistics.dto.TotalMeetPerformanceDTO
import com.terkula.uaxctf.statistics.request.CreateMeetResultRequest
import com.terkula.uaxctf.statistics.response.IndividualMeetSummaryResponse
import com.terkula.uaxctf.statistics.response.RunnerMeetPerformanceResponse
import com.terkula.uaxctf.statistics.service.IndividualMeetSummaryAsync
import com.terkula.uaxctf.statistics.service.IndividualMeetSummaryService
import com.terkula.uaxctf.statistics.service.MeetPerformanceService
import java.sql.Date
import java.time.Year
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RestController
class MeetPerformanceController(@field:Autowired
                                internal var meetPerformanceService: MeetPerformanceService,
                                var individualMeetSummaryService: IndividualMeetSummaryService,
                                var individualMeetSummaryServiceAsync: IndividualMeetSummaryAsync
) {

    @RequestMapping(value = ["xc/getMeetResultByName"], method = [RequestMethod.GET])
    fun getMeetResultsByName(
            @RequestParam(value = "filter.runner") partialName: String,
            @RequestParam(value = "filter.startSeason", required = false, defaultValue = "") startSeason: String,
            @RequestParam(value = "filter.endSeason", required = false, defaultValue = "") endSeason: String,
            @RequestParam(value = "sort.method", required = false, defaultValue = "time") sortMethod: String,
            @RequestParam(value = "page.size", required = false, defaultValue = "10") count: Int,
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

    @RequestMapping(value = ["xc/getMeetResultByMeetName"], method = [RequestMethod.GET])
    fun getMeetResultsByMeetName(
            @RequestParam(value = "filter.meetName") partialName: String,
            @RequestParam(value = "filter.startSeason", required = false, defaultValue = "") startSeason: String,
            @RequestParam(value = "filter.endSeason", required = false, defaultValue = "") endSeason: String,
            @RequestParam(value = "sort.method", required = false, defaultValue = "time") sortMethod: String,
            @RequestParam(value = "page.size", required = false, defaultValue = "10") count: Int,
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

    @RequestMapping(value = ["xc/historicallyCompareMeets"], method = [RequestMethod.GET])
    fun historicallyCompareMeets(
            @RequestParam(value = "baseMeetName") baseMeetName: String,
            @RequestParam(value = "compareMeetName") compareToMeetName: String,
            @RequestParam(value = "excludeSeasons", required = false, defaultValue = "") excludeSeasons: List<String>,
            @RequestParam(value = "includeSeasons", required = false, defaultValue = "") includeSeasons: List<String>,
            @RequestParam(value = "adjust.forDistance", required = false, defaultValue = "false") adjustForDistance: Boolean = false): StatisticalComparisonDTO {

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


    @RequestMapping(value = ["xc/meetResults"], method = [RequestMethod.GET])
    fun getTotalMeetResultsByMeetName(
            @RequestParam(value = "filter.meetName") partialName: String,
            @RequestParam(value = "filter.season", required = false, defaultValue = "") season: String,
            @RequestParam(value = "sort.method", required = false, defaultValue = "time") sortMethod: String,
            @RequestParam(value = "page.size", required = false, defaultValue = "10") count: Int,
            @RequestParam(value = "adjust.forDistance", required = false, defaultValue = "false") adjustForDistance: Boolean = false,
            @RequestParam(value = "team", required = false, defaultValue = "UA") team: String? = "UA"): List<RunnerMeetPerformanceDTO>  {

        var startDate = Date.valueOf("$season-01-01")
        var endDate = Date.valueOf("$season-12-31")

        val sortingMethodContainer = getSortingMethod(sortMethod)

        var teamString = team ?: "UA"

        return meetPerformanceService.getTotalMeetPerformancesAtMeet(partialName,
                startDate, endDate, sortingMethodContainer, count, adjustForDistance, teamString)
    }

    @RequestMapping(value = ["xc/meetResults/createForRunner"], method = [RequestMethod.POST])
    fun createMeetResultForRunner(@RequestBody createMeetResultRequest: CreateMeetResultRequest)
           : List<RunnerMeetPerformanceDTO>  {

        var startDate = Date.valueOf("${createMeetResultRequest.season}-01-01")
        var endDate = Date.valueOf("${createMeetResultRequest.season}-12-31")

        return meetPerformanceService.createMeetResult(createMeetResultRequest)
    }

    @RequestMapping(value = ["xc/individualMeetSummary"], method = [RequestMethod.GET])
    fun getIndividualMeetSummaryForRunner(
            @RequestParam(value = "filter.meetUuid") meetUuid: String,
            @RequestParam(value = "filter.runnerId") runnerId: Int
    )
            : IndividualMeetSummaryResponse? {

        return individualMeetSummaryServiceAsync.getIndividualMeetSummary(runnerId, meetUuid).get()
    }

    @RequestMapping(value = ["xc/individualMeetSummaries"], method = [RequestMethod.GET])
    fun getIndividualMeetSummaryForRunner(
            @RequestParam(value = "filter.meetUuid") meetUuid: String,
    )
            : List<IndividualMeetSummaryResponse> {

        return individualMeetSummaryService.getIndividualMeetSummaries(meetUuid)
    }


}
