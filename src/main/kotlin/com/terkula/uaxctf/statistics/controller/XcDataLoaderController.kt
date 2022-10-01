package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statistics.exception.UnauthenticatedException
import com.terkula.uaxctf.statistics.service.MeetPerformanceService
import io.swagger.annotations.ApiOperation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.core.env.get
import org.springframework.web.bind.annotation.*
import java.sql.Date


@RestController
class XcDataLoaderController(
    @field:Autowired
    internal var meetPerformanceService: MeetPerformanceService,
    @field:Autowired
    internal var env: Environment
) {

    @ApiOperation("Loads meet data that has manually been entered in the load table.")
    @RequestMapping(value = ["xc/load/{meetId}"], method = [RequestMethod.GET])
    fun loadPerformances(
            @PathVariable("meetId") meetId: Int,
            @RequestParam(value = "password") password: String
    ): String {

        if (authenticated(password)) {
            meetPerformanceService.loadMeetPerformance(meetId)
            return "loaded"
        } else {
            throw UnauthenticatedException("The provided password is not valid")
        }
    }

    @ApiOperation("Loads milesplit data for meet that has been entered in the mile split table.")
    @RequestMapping(value = ["xc/load/mileSplit/{meetId}"], method = [RequestMethod.GET])
    fun loadMileSplits(
        @PathVariable("meetId") meetId: Int,
        @RequestParam(value = "password") password: String
    ): String {

        if (authenticated(password)) {
            meetPerformanceService.loadMileSplits(meetId)
            return "loaded mile splits"
        } else {
            throw UnauthenticatedException("The provided password is not valid")
        }
    }

    @ApiOperation("Load Time Trial Data")
    @RequestMapping(value = ["xc/load/TimeTrialResult"], method = [RequestMethod.GET])
    fun loadTimeTrialData(
        @RequestParam(value = "season") season: String,
        @RequestParam(value = "password") password: String
    ): String {

        if (authenticated(password)) {
            meetPerformanceService.loadTimeTrial(season)
            return "loaded time trial"
        } else {
            throw UnauthenticatedException("The provided password is not valid")
        }
    }

    @ApiOperation("Create Meet Info")
    @RequestMapping(value = ["xc/createMeetInfoEntry/"], method = [RequestMethod.POST])
    fun createMeetInfoEntry(
            @RequestParam(value = "meetName") meetName: String,
            @RequestParam(value = "season", required = false, defaultValue = "") season: String = "",
            @RequestParam(value = "distance") distance: Int,
            @RequestParam(value = "elevationChange") elevationChange: Int,
            @RequestParam(value = "humidity") humidity: Double,
            @RequestParam(value = "isRainy", required = false, defaultValue = "false") isRainy: Boolean = false,
            @RequestParam(value = "isSnowy", required = false, defaultValue = "false") isSnowy: Boolean = false,
            @RequestParam(value = "temperature") temperature: Int,
            @RequestParam(value = "windSpeed") windSpeed: Int,
            @RequestParam(value = "cloudCoverRatio") cloudCoverRatio: Double,
            @RequestParam(value = "password") password: String
    ) {
        if (authenticated(password)) {
            var startDate = Date.valueOf("${MeetPerformanceController.CURRENT_YEAR}-01-01")
            var endDate = Date.valueOf((MeetPerformanceController.CURRENT_YEAR) + "-12-31")

            if (season.isNotEmpty()) {
                startDate = Date.valueOf("$season-01-01")
                endDate = Date.valueOf("$season-12-31")
            }

            meetPerformanceService.postMeetInfoEntry(meetName, startDate, endDate, distance, elevationChange,
                    humidity, isRainy, isSnowy, temperature, windSpeed, cloudCoverRatio)
        } else {
            throw UnauthenticatedException("The provided password is not valid")
        }
    }

    @ApiOperation("Read Workout Results From raw result table and post the results")
    @RequestMapping(value = ["xc/readWorkoutResults/"], method = [RequestMethod.GET])
    fun loadWorkoutResults(
            @RequestParam(value = "workoutId") workoutId: Int,
            @RequestParam(value = "password") password: String
    ): String {

        if (authenticated(password)) {
            meetPerformanceService.loadWorkout(workoutId)
            return "loaded"
        } else {
            throw UnauthenticatedException("The provided password is not valid")
        }
    }

    private fun authenticated(password: String): Boolean {
        return password == env["api.auth.password"]
    }

}
