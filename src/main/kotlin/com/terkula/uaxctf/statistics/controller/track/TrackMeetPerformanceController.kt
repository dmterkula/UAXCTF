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
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import java.sql.Date
import java.time.Year
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RestController
class TrackMeetPerformanceController(
        internal var trackMeetPerformanceService: TrackMeetPerformanceService,
        internal var meetInfoService: MeetInfoService
        ) {

//    @ApiOperation("Returns the meet results for all runners matching the given name")
//    @RequestMapping(value = ["xc/getMeetResultByName"], method = [RequestMethod.GET])
//    fun getMeetResultsByName(
//            @ApiParam("Get results for runners whose name contains this input. Can pass partial names to get results for multiple runners. " +
//                    "e.g just passing common last name for sisters '")
//            @RequestParam(value = "filter.runner") partialName: String,
//            @ApiParam("Filters results for meets with a date after 01-01 of the input year")
//            @RequestParam(value = "filter.startSeason", required = false, defaultValue = "") startSeason: String,
//            @ApiParam("Filters results for meets within the start date and 12-31 of the input year")
//            @RequestParam(value = "filter.endSeason", required = false, defaultValue = "") endSeason: String,
//            @ApiParam("Sorts results based on the input valid sorting methods are 'date' and 'time'. " +
//                    "If one these values is not provided, no sort is used ")
//            @RequestParam(value = "sort.method", required = false, defaultValue = "time") sortMethod: String,
//            @ApiParam("Limits the number of total results returned to the input")
//            @RequestParam(value = "page.size", required = false, defaultValue = "10") count: Int,
//            @ApiParam("Adjusts seasons bests for true distance of the meet if value passed is true")
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

//    @ApiOperation("Returns the meet results for the given runner in the given season")
//    @RequestMapping(value = ["xc/getMeetResultByMeetName"], method = [RequestMethod.GET])
//    fun getMeetResultsByMeetName(
//            @ApiParam("Filters results based on runners whose name matches/partially matches the input name'")
//            @RequestParam(value = "filter.meetName") partialName: String,
//            @ApiParam("Filters results for meets with a date after 01-01 of the input year")
//            @RequestParam(value = "filter.startSeason", required = false, defaultValue = "") startSeason: String,
//            @ApiParam("Filters results for meets within the start date and 12-31 of the input year")
//            @RequestParam(value = "filter.endSeason", required = false, defaultValue = "") endSeason: String,
//            @ApiParam("Sorts results based on the input valid sorting methods are 'date' and 'time'. " +
//                    "If one these values is not provided, no sort is used ")
//            @RequestParam(value = "sort.method", required = false, defaultValue = "time") sortMethod: String,
//            @ApiParam("Limits the number of total results returned to the input")
//            @RequestParam(value = "page.size", required = false, defaultValue = "10") count: Int,
//            @ApiParam("Adjusts seasons bests for true distance of the meet if value passed is true")
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


//    @ApiOperation("Returns the meet results for all runners matching the given name")
//    @RequestMapping(value = ["xc/meetResults"], method = [RequestMethod.GET])
//    fun getTotalMeetResultsByMeetName(
//            @ApiParam("Filters results based on runners whose name matches/partially matches the input name'")
//            @RequestParam(value = "filter.meetName") partialName: String,
//            @ApiParam("Filters results for meets with a date after 01-01 of the input year")
//            @RequestParam(value = "filter.season", required = false, defaultValue = "") season: String,
//            @ApiParam("Sorts results based on the input valid sorting methods are 'date' and 'time'. " +
//                    "If one these values is not provided, no sort is used ")
//            @RequestParam(value = "sort.method", required = false, defaultValue = "time") sortMethod: String,
//            @ApiParam("Limits the number of total results returned to the input")
//            @RequestParam(value = "page.size", required = false, defaultValue = "10") count: Int,
//            @ApiParam("Adjusts seasons bests for true distance of the meet if value passed is true")
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

    @ApiOperation("Logs meet results for a runner")
    @RequestMapping(value = ["track/meetResults/createForRunner"], method = [RequestMethod.POST])
    fun createMeetResultForRunner(@RequestBody createTrackMeetResultRequest: CreateTrackMeetResultRequest)
           : TrackMeetPerformanceDTO?  {

        return trackMeetPerformanceService.createTrackMeetResult(createTrackMeetResultRequest)
    }

    @ApiOperation("Returns all the meet results at a given meet")
    @RequestMapping(value = ["track/meetResults"], method = [RequestMethod.GET])
    fun getMeetResults(@RequestParam(value = "meetUUID") meetUUID: String)
            : List<TrackMeetPerformanceDTO>  {

        return trackMeetPerformanceService.getTrackMeetResults(meetUUID)
    }

    @ApiOperation("Returns all the meet results at a given meet given a season and meet name")
    @RequestMapping(value = ["track/meetResults"], method = [RequestMethod.GET])
    fun getMeetResults(@RequestParam(value = "meetName") meetName: String,
                       @RequestParam(value = "season") season: String)
            : List<TrackMeetPerformanceDTO>  {

        return trackMeetPerformanceService.getTrackMeetResults(meetName, season)
    }


}
