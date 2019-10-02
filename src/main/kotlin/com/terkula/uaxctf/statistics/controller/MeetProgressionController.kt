package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statistics.response.MeetProgressionResponse

import com.terkula.uaxctf.statistics.service.MeetProgressionService
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

@Validated
@RestController
class MeetProgressionController(@field:Autowired
                                internal var meetProgressionService: MeetProgressionService) {


    @ApiOperation("Returns how much time the given runner has taken off or added between the same meet across seasons")
    @RequestMapping(value = ["xc/meetProgression/interSeason"], method = [RequestMethod.GET])
    fun getMeetProgressionForRunner(
            @RequestParam(value = "filter.meet", required = true) meetName: String,
            @RequestParam(value = "filter.runner") runnerName: String) : MeetProgressionResponse {

        val progessions = meetProgressionService.getSingleMeetSingleRunnerProgression(meetName, runnerName)
        return MeetProgressionResponse(progessions.size, progessions)

    }

    @ApiOperation("Returns how much time every runner has taken off or added between the same meet across seasons")
    @RequestMapping(value = ["xc/meetProgression/interSeason/forAllRunnersAt"], method = [RequestMethod.GET])
    fun getAllProgressionsFromMeet(
            @ApiParam("Filters results for meets matching the given name/partial name")
            @RequestParam(value = "filter.meet", required = true) meetName: String,
            @ApiParam("Filters results for meets with a date after 01-01 of the input year")
            @RequestParam(value = "filter.startYear", defaultValue = "") startYear: String,
            @ApiParam("Filters results for meets within the start date and 12-31 of the input year")
            @RequestParam(value = "filter.endYear", defaultValue = "") endYear: String,
            @ApiParam("Filters results for progressions which are 'faster' or 'slower' depending on what is input")
            @Pattern(
                    regexp = "faster|slower",
                    message = "The value provided for filter.time is invalid. Valid values are 'faster' or 'slower' or no value")
            @RequestParam(value = "filter.time", defaultValue = "") filterBy: String): MeetProgressionResponse {

        var startDate = Date.valueOf((MeetPerformanceController.CURRENT_YEAR.toInt()-1).toString() + "-01-01")
        var endDate = Date.valueOf((MeetPerformanceController.CURRENT_YEAR) + "-12-31")

        if (startYear.isNotEmpty()) {
            startDate = Date.valueOf("$startYear-01-01")
            endDate = Date.valueOf("$endYear-12-31")
        }

        val progressions = meetProgressionService.getProgressionFromMeetForAllRunnersBetweenDates(meetName, startDate, endDate, filterBy)
        return MeetProgressionResponse(progressions.size, progressions)

    }


    @ApiOperation("Returns how much time every runner has taken off between meets this season")
    @RequestMapping(value = ["xc/meetProgression/intraSeason"], method = [RequestMethod.GET])
    fun getIntraSeasonProgressionsFromMeet(
            @RequestParam(value = "numMeets", defaultValue = "2") numMeets: Int,
            @Pattern(
                    regexp = "faster|slower|",
                    message = "The value provided for filter.time is invalid. Valid values are 'faster' or 'slower' or no value")
            @RequestParam(value = "filter.time", defaultValue = "") filterBy: String,
            @RequestParam(value = "filter.excludeMeet", defaultValue = "") excludedMeet: String): MeetProgressionResponse {

        val startDate = Date.valueOf(MeetPerformanceController.CURRENT_YEAR + "-01-01")
        val endDate = Date.valueOf((MeetPerformanceController.CURRENT_YEAR) + "-12-31")

        val progressions = meetProgressionService.getMeetProgressionsFromLastNMeets(numMeets, MeetPerformanceController.CURRENT_YEAR, startDate, endDate, filterBy, excludedMeet)

        return MeetProgressionResponse(progressions.size, progressions)

    }

}
