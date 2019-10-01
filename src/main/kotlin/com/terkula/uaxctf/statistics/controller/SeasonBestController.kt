package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statistics.response.SeasonBestResponse
import com.terkula.uaxctf.statistics.service.SeasonBestService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import java.sql.Date
import java.util.stream.IntStream
import kotlin.streams.toList

@RestController()
class SeasonBestController(@field:Autowired
                   internal var seasonBestService: SeasonBestService) {


    @RequestMapping(value = ["/seasonBests/all"], method = [RequestMethod.GET])
    fun getAllSeasonBestsByYear(
    @RequestParam(value = "season", required = false, defaultValue = "") season: String) : SeasonBestResponse {

        var startDate = Date.valueOf("${MeetPerformanceController.CURRENT_YEAR}-01-01")
        var endDate = Date.valueOf((MeetPerformanceController.CURRENT_YEAR) + "-12-31")


        if (season.isNotEmpty()) {
            startDate = Date.valueOf("$season-01-01")
            endDate = Date.valueOf("$season-12-31")
        }

        val seasonBests = seasonBestService.findWhoseSeasonBestIsFirstMeet(startDate, endDate)

        return SeasonBestResponse(seasonBests.size, seasonBests)

    }

    @RequestMapping(value = ["/seasonBests/"], method = [RequestMethod.GET])
    fun getRunnerSeasonBestsByYear(
            @RequestParam(value = "numYears", required = false, defaultValue = "1") numSeasons: Int,
            @RequestParam(value = "name.contains") partialName: String) : SeasonBestResponse {

        val startEndPairs = IntStream.range(1, numSeasons + 1).toList().map {
            val startSeasonYear = MeetPerformanceController.CURRENT_YEAR.toInt() - (it - 1)
            var startDate = Date.valueOf("$startSeasonYear-01-01")
            var endDate = Date.valueOf("$startSeasonYear-12-31")
            startDate to endDate
        }

        val seasonBests = seasonBestService.getSeasonBestsByName(partialName, startEndPairs)

        return SeasonBestResponse(seasonBests.size, seasonBests)

    }

    @RequestMapping(value = ["/seasonBests/lastMeet"], method = [RequestMethod.GET])
    fun getSeasonBestsAtLastMeet(): SeasonBestResponse {

        var startDate = Date.valueOf("${MeetPerformanceController.CURRENT_YEAR}-01-01")
        var endDate = Date.valueOf((MeetPerformanceController.CURRENT_YEAR) + "-12-31")

        val seasonBests = seasonBestService.getSeasonBestsAtLastMeet(startDate, endDate)

        return SeasonBestResponse(seasonBests.size, seasonBests)

    }


    @RequestMapping(value = ["/seasonBests/isAtFirstMeet"], method = [RequestMethod.GET])
    fun findWhoseSeasonBestIsFirstMeet(): SeasonBestResponse {

        var startDate = Date.valueOf("${MeetPerformanceController.CURRENT_YEAR}-01-01")
        var endDate = Date.valueOf((MeetPerformanceController.CURRENT_YEAR) + "-12-31")


        val seasonBests = seasonBestService.findWhoseSeasonBestIsFirstMeet(startDate, endDate)

        return SeasonBestResponse(seasonBests.size, seasonBests)

    }


}
