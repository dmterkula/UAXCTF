package com.terkula.uaxctf.training.controller

import com.terkula.uaxctf.statistics.controller.MeetPerformanceController
import com.terkula.uaxctf.training.response.RunnerWorkoutResultsResponse
import com.terkula.uaxctf.training.response.WorkoutResultResponse
import com.terkula.uaxctf.training.service.WorkoutResultService
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.sql.Date
import javax.validation.constraints.Pattern

@RestController
class WorkoutResultController (@field:Autowired
                               internal val workoutResultService: WorkoutResultService) {

    @ApiOperation("Returns the results for a workout on a given day")
    @RequestMapping(value = ["/workouts/results"], method = [RequestMethod.GET])
    fun getResultsForWorkout(
            @ApiParam("Valid values for type are 'interval', 'tempo' or 'progression'")
            @Pattern(
                    regexp = "interval|tempo|progression",
                    message = "The value provided for type is invalid. Valid values are 'progression', 'tempo' or 'progression'")
            @RequestParam(value = "type", required = true) workoutType: String,
            @ApiParam("The date of the workout: yyyy-mm-dd")
            @RequestParam(value = "filter.date", required = true) date: String,
            @ApiParam("The distance of the workout in meters. Mile workouts distance is 1609 meters")
            @RequestParam(value = "filter.distance", required = false) distance: Int,
            @ApiParam("The target pace for the workout, as based upon the following provided value: 'Goal' or 'Race Pace'")
            @Pattern(
                    regexp = "Goal|Race Pace",
                    message = "The value provided for target pace is invalid. Valid values are 'Goal' or 'Race Pace'")
            @RequestParam(value = "filter.pace", required = true) pace: String,
            @Pattern(
                    regexp = "spread|target|differential",
                    message = "The value provided for sort.method is invalid. Accepted values are 'spread', 'target', or 'differential'")
            @ApiParam("Optional sort parameter to sort the list of workouts based on the attribute, defaulting to target pace." +
                    " Accepted values are 'spread', 'target', or 'differential'")
            @RequestParam(value = "sort.method", required = false, defaultValue = "target") sortMethod: String = "target"): WorkoutResultResponse {

        return WorkoutResultResponse(workoutResultService.getWorkoutResults(Date.valueOf(date), workoutType, distance, pace, sortMethod))
    }

//    @ApiOperation("Returns the workout results for a given runner")
//    @RequestMapping(value = ["/workoutResults/forRunner"], method = [RequestMethod.GET])
//    fun getWorkoutsForRunner(
//            @ApiParam("Optional parameter to filter for workouts of a given type. Valid values are 'progression', 'tempo' or 'progression")
//            @Pattern(
//                    regexp = "interval|tempo|progression",
//                    message = "The value provided for filter.type is invalid. Valid values are 'progression', 'tempo' or 'progression'")
//            @RequestParam(value = "filter.type", required = false) workoutType: String = "",
//            @ApiParam("The name of the runner")
//            @RequestParam(value = "filter.runner", required = true) name: String,
//            @ApiParam("Filters workouts to within a given season")
//            @RequestParam(value = "filter.season", required = false, defaultValue = "") season: String,
//            @RequestParam(value = "filter.distance", required = false) distance: Int = 0,
//            @Pattern(
//                    regexp = "spread|target|differential",
//                    message = "The value provided for sort.method is invalid. Accepted values are 'spread', 'target', or 'differential'")
//            @ApiParam("Optional sort parameter to sort the list of workouts based on the attribute, defaulting to target pace." +
//                    " Accepted values are 'spread', 'target', or 'differential'")
//            @RequestParam(value = "sort.method", required = false, defaultValue = "target") sortMethod: String = "target"): RunnerWorkoutResultsResponse {
//
//        var startDate = Date.valueOf("${MeetPerformanceController.CURRENT_YEAR}-01-01")
//        var endDate = Date.valueOf("${MeetPerformanceController.CURRENT_YEAR}-12-31")
//
//        if (season.isNotEmpty()) {
//            startDate = Date.valueOf("$season-01-01")
//            endDate = Date.valueOf("$season-12-31")
//        }
//
//        return workoutResultService.getWorkoutsForRunner(startDate, endDate, name, distance, workoutType, sortMethod)
//    }
}