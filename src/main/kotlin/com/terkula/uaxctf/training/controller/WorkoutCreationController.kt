package com.terkula.uaxctf.training.controller

import com.terkula.uaxctf.statistics.controller.MeetPerformanceController
import com.terkula.uaxctf.training.response.WorkoutCreationResponse
import com.terkula.uaxctf.training.service.WorkoutCreationService
import io.swagger.annotations.ApiParam
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
class WorkoutCreationController(@field:Autowired
                               internal var workoutCreationService: WorkoutCreationService) {

    @RequestMapping(value = ["/workoutCreator"], method = [RequestMethod.GET])
    fun planWorkout(
            @ApiParam("Valid values for type are 'interval', 'tempo' or 'progression'")
            @Pattern(
                    regexp = "interval|tempo|progression",
                    message = "The value provided for type is invalid. Valid values are 'progression', 'tempo' or 'progression'")
            @RequestParam(value = "type", required = true) workoutType: String,
            @ApiParam("The entered distance should be in meters, this field is ignored for tempos and progression types")
            @RequestParam(value = "distance", required = true) distance: Int,
            @ApiParam("The target pace for the workout, as based upon the following provided value: 'goal', 'pr' 'seasonBest' or 'seasonBestAverage'")
            @Pattern(
                    regexp = "goal|pr|seasonBest|seasonBestAverage",
                    message = "The value provided for pace is invalid. Valid values are 'goal', 'pr' or 'seasonBest', or 'seasonBestAverage'")
            @RequestParam(value = "pace", required = true) pace: String
                           ): WorkoutCreationResponse? {

        var startDate = Date.valueOf("${MeetPerformanceController.CURRENT_YEAR}-01-01")
        var endDate = Date.valueOf((MeetPerformanceController.CURRENT_YEAR) + "-12-31")

        return workoutCreationService.createWorkout(workoutType, distance, pace)


    }

}