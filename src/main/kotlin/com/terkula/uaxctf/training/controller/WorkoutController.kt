package com.terkula.uaxctf.training.controller

import com.terkula.uaxctf.training.model.Workout
import com.terkula.uaxctf.training.response.WorkoutCreationResponse
import com.terkula.uaxctf.training.service.WorkoutService
import com.terkula.uaxctf.training.service.WorkoutGroupBuilderService
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

@RestController
@Validated
class WorkoutController(
        @field:Autowired
    internal var workoutService: WorkoutService,
        @field:Autowired
    internal var workoutGroupBuilderService: WorkoutGroupBuilderService
) {

    @ApiOperation("Returns a planned workout for each runner based on workout type and the given pace and distance parameters")
    @RequestMapping(value = ["xc/workout/create"], method = [RequestMethod.POST])
    fun planWorkout(
            @ApiParam("Valid values for type are 'Interval', 'Tempo' or 'Progression'")
            @Pattern(
                    regexp = "Interval|Tempo|Progression|descriptionOnly",
                    message = "The value provided for type is invalid. Valid values are 'Interval', 'Tempo' or 'Progression'")
            @RequestParam(value = "type", required = true) workoutType: String,

            @ApiParam("Date in the form of year-month-day")
            @RequestParam(value = "date", required = true) date: Date,

            @ApiParam("Workout Title")
            @RequestParam(value = "title", required = true) title: String,

            @ApiParam("Workout Description")
            @RequestParam(value = "description", required = false, defaultValue = "") description: String,

            @ApiParam("The entered distance should be in meters, this field is ignored for tempos and progression types")
            @RequestParam(value = "distance", required = true) distance: Int,

            @ApiParam("The number of reps. 0 if not applicable")
            @RequestParam(value = "count", required = true) count: Int,

            @ApiParam("Workout Description")
            @RequestParam(value = "duration", required = false, defaultValue = "") duration: String,

            @ApiParam("Workout uuid")
            @RequestParam(value = "uuid", required = true) uuid: String,

            @ApiParam("Workout icon")
            @RequestParam(value = "icon", required = false, defaultValue = "") icon: String,

            @ApiParam("The target pace for the workout, as based upon the following provided value: 'goal', 'pr' 'seasonBest' or 'seasonBestAverage'")
            @Pattern(
                    regexp = "Goal|PR|SB|Season Avg",
                    message = "The value provided for pace is invalid. Valid values are 'Goal', 'PR' or 'SB', or 'Season Avg'")
            @RequestParam(value = "pace", required = true) pace: String): WorkoutCreationResponse? {

        return workoutService.createWorkout(date, workoutType, title, description, distance, count, pace, duration, icon, uuid)
    }

    @ApiOperation("Returns a planned workout for each runner based on workout type and the given pace and distance parameters")
    @RequestMapping(value = ["xc/workout/update"], method = [RequestMethod.POST])
    fun updateWorkout(
            @ApiParam("Valid values for type are 'Interval', 'Tempo' or 'Progression'")
            @Pattern(
                    regexp = "Interval|Tempo|Progression|descriptionOnly",
                    message = "The value provided for type is invalid. Valid values are 'Interval', 'Tempo' or 'Progression'")
            @RequestParam(value = "type", required = true) workoutType: String,

            @ApiParam("Date in the form of year-month-day")
            @RequestParam(value = "date", required = true) date: Date,

            @ApiParam("Workout Title")
            @RequestParam(value = "title", required = true) title: String,

            @ApiParam("Workout uuid")
            @RequestParam(value = "uuid", required = true) uuid: String,

            @ApiParam("Workout icon")
            @RequestParam(value = "icon", required = false, defaultValue = "") icon: String,

            @ApiParam("Workout Description")
            @RequestParam(value = "description", required = false, defaultValue = "") description: String,

            @ApiParam("The entered distance should be in meters, this field is ignored for tempos and progression types")
            @RequestParam(value = "distance", required = true) distance: Int,

            @ApiParam("The number of reps. 0 if not applicable")
            @RequestParam(value = "count", required = true) count: Int,

            @ApiParam("Workout Description")
            @RequestParam(value = "duration", required = false, defaultValue = "") duration: String,

            @ApiParam("The target pace for the workout, as based upon the following provided value: 'Goal', 'PR' 'SB' or 'Season Avg'")
            @Pattern(
                    regexp = "Goal|PR|SB|Season Avg",
                    message = "The value provided for pace is invalid. Valid values are 'Goal', 'PR' or 'SB', or 'Season Avg'")
            @RequestParam(value = "pace", required = true) pace: String): Workout? {

        return workoutService.updateWorkout(uuid, date, workoutType, title, description, distance, count, pace, duration, icon)
    }

    @ApiOperation("Returns a planned workout for each runner based on workout type and the given pace and distance parameters")
    @RequestMapping(value = ["xc/workout/get"], method = [RequestMethod.GET])
    fun getWorkouts(

            @ApiParam("Earliest Date to look for")
            @RequestParam(value = "startDate", required = true) startDate: Date,

            @ApiParam("Latest date to look for")
            @RequestParam(value = "endDate", required = true) endDate: Date,

           ): List<Workout> {

        return workoutService.getWorkouts(startDate, endDate)
    }

    @ApiOperation("Delete a workout on a given day with the matching title")
    @RequestMapping(value = ["xc/workout/delete"], method = [RequestMethod.DELETE])
    fun getWorkouts(

            @ApiParam("uuid")
            @RequestParam(value = "uuid", required = true) uuid: String,

            ): Workout? {

        return workoutService.deleteWorkout(uuid)
    }

    @ApiOperation("Returns workout groups")
    @RequestMapping(value = ["/workout/groups"], method = [RequestMethod.GET])
    fun buildWorkoutGroups(
            @ApiParam("the max spread of the group in 5k time in seconds")
            @RequestParam(value = "maxSpread", required = false, defaultValue = "30") maxSpread: Int,
            @ApiParam("Minimum group size")
            @RequestParam(value = "minGroupSize", required = true) minGroupSize: Int,
            @ApiParam("Maximum group size")
            @RequestParam(value = "maxGroupSize", required = true) maxGroupSize: Int,
            @ApiParam("season, e.g: 2020")
            @RequestParam(value = "season", required = true) season: String,
            @ApiParam("Allowed Fuzziness Factor. This parameter checks to see if groups of size one " +
                    "can be merged with other groups if the merging the groups is not greater than " +
                    "maxSpread * fuzzinessFactor")
            @RequestParam(value = "fuzzinessFactor", required = true) fuzzinessFactor: Double
           ) {

        workoutGroupBuilderService.buildWorkoutGroups(season, maxSpread, minGroupSize, maxGroupSize, fuzzinessFactor)
    }

}