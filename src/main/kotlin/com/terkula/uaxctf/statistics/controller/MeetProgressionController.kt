package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statistics.response.MeetProgressionResponse

import com.terkula.uaxctf.statistics.service.MeetProgressionService
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


    @RequestMapping(value = ["/meetProgression/interSeason"], method = [RequestMethod.GET])
    fun getMeetProgressionForRunner(
            @RequestParam(value = "filter.meet", required = true) meetName: String,
            @RequestParam(value = "filter.runner") runnerName: String) : MeetProgressionResponse {

        val progessions = meetProgressionService.getSingleMeetSingleRunnerProgression(meetName, runnerName)
        return MeetProgressionResponse(progessions.size, progessions)

    }

    @RequestMapping(value = ["/meetProgression/interSeason/forAllRunnersAt"], method = [RequestMethod.GET])
    fun getAllProgressionsFromMeet(
            @RequestParam(value = "filter.meet", required = true) meetName: String,
            @RequestParam(value = "filter.startYear", defaultValue = "") startYear: String,
            @RequestParam(value = "filter.endYear", defaultValue = "") endYear: String,
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


    @RequestMapping(value = ["/meetProgression/intraSeason"], method = [RequestMethod.GET])
    fun getInterSeasonProgressionsFromMeet(
            @RequestParam(value = "numMeets", defaultValue = "2") numMeets: Int,
            @Pattern(
                    regexp = "faster|slower|",
                    message = "The value provided for filter.time is invalid. Valid values are 'faster' or 'slower' or no value")
            @RequestParam(value = "filter.time", defaultValue = "") filterBy: String,
            @RequestParam(value = "filter.excludeMeet", defaultValue = "") excludedMeet: String): MeetProgressionResponse {

        var startDate = Date.valueOf(MeetPerformanceController.CURRENT_YEAR + "-01-01")
        var endDate = Date.valueOf((MeetPerformanceController.CURRENT_YEAR) + "-12-31")

        val progressions = meetProgressionService.getMeetProgressionsFromLastNMeets(numMeets, MeetPerformanceController.CURRENT_YEAR, startDate, endDate, filterBy, excludedMeet)

        return MeetProgressionResponse(progressions.size, progressions)

    }

}
