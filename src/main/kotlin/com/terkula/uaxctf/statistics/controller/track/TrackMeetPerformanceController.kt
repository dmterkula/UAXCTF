package com.terkula.uaxctf.statistics.controller.track

import com.terkula.uaxctf.statisitcs.model.Meet
import com.terkula.uaxctf.statisitcs.model.track.TrackMeet
import com.terkula.uaxctf.statisitcs.model.track.TrackMeetPerformanceDTO
import com.terkula.uaxctf.statistics.dto.RunnerMeetPerformanceDTO
import com.terkula.uaxctf.statistics.request.SortingMethodContainer
import com.terkula.uaxctf.statistics.dto.StatisticalComparisonDTO
import com.terkula.uaxctf.statistics.dto.TotalMeetPerformanceDTO
import com.terkula.uaxctf.statistics.request.CreateMeetRequest
import com.terkula.uaxctf.statistics.request.CreateMeetResultRequest
import com.terkula.uaxctf.statistics.request.track.CreateTrackMeetResultRequest
import com.terkula.uaxctf.statistics.response.RunnerMeetPerformanceResponse
import com.terkula.uaxctf.statistics.service.MeetInfoService
import com.terkula.uaxctf.statistics.service.MeetPerformanceService
import com.terkula.uaxctf.statistics.service.track.TrackMeetPerformanceService
import java.sql.Date
import java.time.Year
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RestController
class TrackMeetPerformanceController(
        internal var trackMeetPerformanceService: TrackMeetPerformanceService,
        internal var meetInfoService: MeetInfoService
        ) {

//    @RequestMapping(value = ["xc/getMeetResultByName"], method = [RequestMethod.GET])
//    fun getMeetResultsByName(
//            @RequestParam(value = "filter.runner") partialName: String,
//            @RequestParam(value = "filter.startSeason", required = false, defaultValue = "") startSeason: String,
//            @RequestParam(value = "filter.endSeason", required = false, defaultValue = "") endSeason: String,
//            @RequestParam(value = "sort.method", required = false, defaultValue = "time") sortMethod: String,
//            @RequestParam(value = "page.size", required = false, defaultValue = "10") count: Int,
//            @RequestParam(value = "adjust.forDistance", required = false, defaultValue = "false") adjustForDistance: Boolean = false): RunnerMeetPerformanceResponse {
//
//        var startDate = Date.valueOf("$CURRENT_YEAR-01-01")
//        var endDate = Date.valueOf("$CURRENT_YEAR-12-31")
//
//        if (startSeason.isNotEmpty()) {
//            startDate = Date.valueOf("$startSeason-01-01")
//        }
//
//        if (endSeason.isNotEmpty()) {
//            endDate = Date.valueOf("$endSeason-12-31")
//        }
//
//        val sortingMethodContainer = getSortingMethod(sortMethod)
//
//        return RunnerMeetPerformanceResponse(meetPerformanceService.getMeetPerformancesForRunnerWithNameContaining(partialName,
//                startDate, endDate, sortingMethodContainer, count, adjustForDistance))
//    }

//    @RequestMapping(value = ["xc/getMeetResultByMeetName"], method = [RequestMethod.GET])
//    fun getMeetResultsByMeetName(
//            @RequestParam(value = "filter.meetName") partialName: String,
//            @RequestParam(value = "filter.startSeason", required = false, defaultValue = "") startSeason: String,
//            @RequestParam(value = "filter.endSeason", required = false, defaultValue = "") endSeason: String,
//            @RequestParam(value = "sort.method", required = false, defaultValue = "time") sortMethod: String,
//            @RequestParam(value = "page.size", required = false, defaultValue = "10") count: Int,
//            @RequestParam(value = "adjust.forDistance", required = false, defaultValue = "false") adjustForDistance: Boolean = false): RunnerMeetPerformanceResponse {
//
//        var startDate = Date.valueOf("$CURRENT_YEAR-01-01")
//        var endDate = Date.valueOf("$CURRENT_YEAR-12-31")
//
//        if (startSeason.isNotEmpty()) {
//            startDate = Date.valueOf("$startSeason-01-01")
//        }
//
//        if (endSeason.isNotEmpty()) {
//            endDate = Date.valueOf("$endSeason-12-31")
//        }
//
//        val sortingMethodContainer = getSortingMethod(sortMethod)
//
//        return RunnerMeetPerformanceResponse(meetPerformanceService.getMeetPerformancesAtMeetName(partialName, startDate,
//                endDate, sortingMethodContainer, count, adjustForDistance))
//    }

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


//    @RequestMapping(value = ["xc/meetResults"], method = [RequestMethod.GET])
//    fun getTotalMeetResultsByMeetName(
//            @RequestParam(value = "filter.meetName") partialName: String,
//            @RequestParam(value = "filter.season", required = false, defaultValue = "") season: String,
//            @RequestParam(value = "sort.method", required = false, defaultValue = "time") sortMethod: String,
//            @RequestParam(value = "page.size", required = false, defaultValue = "10") count: Int,
//            @RequestParam(value = "adjust.forDistance", required = false, defaultValue = "false") adjustForDistance: Boolean = false): List<RunnerMeetPerformanceDTO>  {
//
//        var startDate = Date.valueOf("$season-01-01")
//        var endDate = Date.valueOf("$season-12-31")
//
//        val sortingMethodContainer = getSortingMethod(sortMethod)
//
//        return meetPerformanceService.getTotalMeetPerformancesAtMeet(partialName,
//                startDate, endDate, sortingMethodContainer, count, adjustForDistance)
//    }

    @RequestMapping(value = ["track/meetResults/createForRunner"], method = [RequestMethod.POST])
    fun createMeetResultForRunner(@RequestBody createTrackMeetResultRequest: CreateTrackMeetResultRequest)
           : TrackMeetPerformanceDTO?  {

        return trackMeetPerformanceService.createTrackMeetResult(createTrackMeetResultRequest)
    }

    @RequestMapping(value = ["track/meetResults"], method = [RequestMethod.GET])
    fun getMeetResults(@RequestParam(value = "meetUUID") meetUUID: String)
            : List<TrackMeetPerformanceDTO>  {

        return trackMeetPerformanceService.getTrackMeetResults(meetUUID)
    }

    @RequestMapping(value = ["track/meetResultsByName"], method = [RequestMethod.GET])
    fun getMeetResults(@RequestParam(value = "meetName") meetName: String,
                       @RequestParam(value = "season") season: String)
            : List<TrackMeetPerformanceDTO>  {

        return trackMeetPerformanceService.getTrackMeetResults(meetName, season)
    }


}
