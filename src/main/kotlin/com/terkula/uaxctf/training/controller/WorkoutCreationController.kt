package com.terkula.uaxctf.training.controller

import com.terkula.uaxctf.training.response.WorkoutCreationResponse
import com.terkula.uaxctf.training.service.WorkoutCreationService
import com.terkula.uaxctf.training.service.WorkoutGroupBuilderService
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.validation.constraints.Pattern

@RestController
@Validated
class WorkoutCreationController(
    @field:Autowired
    internal var workoutCreationService: WorkoutCreationService,
    @field:Autowired
    internal var workoutGroupBuilderService: WorkoutGroupBuilderService
) {

    @ApiOperation("Returns a planned workout for each runner based on workout type and the given pace and distance parameters")
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
                    regexp = "goal|pr|seasonBest|seasonAverage",
                    message = "The value provided for pace is invalid. Valid values are 'goal', 'pr' or 'seasonBest', or 'seasonBestAverage'")
            @RequestParam(value = "pace", required = true) pace: String): WorkoutCreationResponse? {

        return workoutCreationService.createWorkout(workoutType, distance, pace)
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