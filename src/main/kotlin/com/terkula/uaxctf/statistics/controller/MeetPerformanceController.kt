package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statistics.request.SortingMethodContainer
import com.terkula.uaxctf.statistics.response.RunnerMeetPerformanceResponse
import com.terkula.uaxctf.statistics.service.MeetPerformanceService
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


    @RequestMapping(value = ["/getMeetResultByName"], method = [RequestMethod.GET])
    fun getMeetResultsByName(@RequestParam(value = "firstName") firstName: String,
                             @RequestParam(value = "lastName") lastName: String,
                             @RequestParam(value = "startSeason", required = false, defaultValue = "") startSeason: String,
                             @RequestParam(value = "endSeason", required = false, defaultValue = "") endSeason: String,
                             @RequestParam(value = "sortMethod", required = false, defaultValue = "time") sortMethod: String,
                             @RequestParam(value = "count", required = false, defaultValue = "10") count: Int): RunnerMeetPerformanceResponse {

        var startDate = Date.valueOf("$CURRENT_YEAR-01-01")
        var endDate = Date.valueOf("$CURRENT_YEAR-12-31")

        if (!startSeason.isEmpty()) {
            startDate = Date.valueOf("$startSeason-01-01")
        }

        if (!endSeason.isEmpty()) {
            endDate = Date.valueOf("$endSeason-12-31")
        }

        val sortingMethodContainer = getSortingMethod(sortMethod)

        return meetPerformanceService.getMeetPerformancesForRunnerWithName(firstName, lastName, startDate, endDate, sortingMethodContainer, count)
    }


    @RequestMapping(value = ["/getMeetResultByNameContaining"], method = [RequestMethod.GET])
    fun getMeetResultsByName(@RequestParam(value = "filter.runner") partialName: String,
                             @RequestParam(value = "filter.startSeason", required = false, defaultValue = "") startSeason: String,
                             @RequestParam(value = "filter.endSeason", required = false, defaultValue = "") endSeason: String,
                             @RequestParam(value = "sort.method", required = false, defaultValue = "time") sortMethod: String,
                             @RequestParam(value = "page.size", required = false, defaultValue = "10") count: Int): RunnerMeetPerformanceResponse {

        var startDate = Date.valueOf("$CURRENT_YEAR-01-01")
        var endDate = Date.valueOf("$CURRENT_YEAR-12-31")

        if (startSeason.isNotEmpty()) {
            startDate = Date.valueOf("$startSeason-01-01")
        }

        if (endSeason.isNotEmpty()) {
            endDate = Date.valueOf("$endSeason-12-31")
        }

        val sortingMethodContainer = getSortingMethod(sortMethod)

        return meetPerformanceService.getMeetPerformancesForRunnerWithNameContaining(partialName, startDate, endDate,  sortingMethodContainer, count)
    }

    @RequestMapping(value = ["/getMeetResultByMeetName"], method = [RequestMethod.GET])
    fun getMeetResultsByMeetName(@RequestParam(value = "filter.meetName") partialName: String,
                             @RequestParam(value = "filter.startSeason", required = false, defaultValue = "") startSeason: String,
                             @RequestParam(value = "filter.endSeason", required = false, defaultValue = "") endSeason: String,
                             @RequestParam(value = "sort.method", required = false, defaultValue = "time") sortMethod: String,
                             @RequestParam(value = "page.size", required = false, defaultValue = "10") count: Int): RunnerMeetPerformanceResponse {

        var startDate = Date.valueOf("$CURRENT_YEAR-01-01")
        var endDate = Date.valueOf("$CURRENT_YEAR-12-31")

        if (startSeason.isNotEmpty()) {
            startDate = Date.valueOf("$startSeason-01-01")
        }

        if (endSeason.isNotEmpty()) {
            endDate = Date.valueOf("$endSeason-12-31")
        }

        val sortingMethodContainer = getSortingMethod(sortMethod)

        return meetPerformanceService.getMeetPerformancesAtMeetName(partialName, startDate, endDate,  sortingMethodContainer, count)
    }


    private fun getSortingMethod(sortMethod: String) =
            when (sortMethod) {
                "date" -> SortingMethodContainer.OLDER_DATE
                "time" -> SortingMethodContainer.TIME
                else -> SortingMethodContainer.NOSORT
            }

    companion object {

        val CURRENT_YEAR = Year.now().toString()
    }


}
