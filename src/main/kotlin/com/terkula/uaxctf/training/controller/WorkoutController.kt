package com.terkula.uaxctf.training.controller

import com.terkula.uaxctf.training.model.Workout
import com.terkula.uaxctf.training.request.CreateWorkoutRequest
import com.terkula.uaxctf.training.response.*
import com.terkula.uaxctf.training.service.WorkoutService
import com.terkula.uaxctf.training.service.WorkoutGroupBuilderService
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.sql.Date
import javax.validation.Valid
import javax.validation.constraints.Pattern

@RestController
@Validated
class WorkoutController(
        @field:Autowired
    internal var workoutService: WorkoutService,
        @field:Autowired
    internal var workoutGroupBuilderService: WorkoutGroupBuilderService
) {

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

    @ApiOperation("Returns a planned workout for each runner based on workout type and the given pace and distance parameters")
    @RequestMapping(value = ["xc/workout/get"], method = [RequestMethod.GET])
    fun getWorkoutsV2(

            @ApiParam("Earliest Date to look for")
            @RequestParam(value = "startDate", required = true) startDate: Date,

            @ApiParam("Latest date to look for")
            @RequestParam(value = "endDate", required = true) endDate: Date,

            ): List<WorkoutResponseDTO> {

        return workoutService.getWorkoutsV2(startDate, endDate)
    }

    @ApiOperation("Returns a planned workout for each runner based on workout type and the given pace and distance parameters")
    @RequestMapping(value = ["xc/workout/create"], method = [RequestMethod.POST])
    fun createWorkoutV2(
           @RequestBody @Valid createWorkoutRequest: CreateWorkoutRequest
    ): WorkoutResponseDTO {
        return workoutService.createWorkoutV2(createWorkoutRequest)
    }

    @ApiOperation("Returns a planned workout for each runner based on workout type and the given pace and distance parameters")
    @RequestMapping(value = ["xc/workout/update"], method = [RequestMethod.PUT])
    fun updateWorkoutV2(
            @RequestParam workoutUUID: String,
            @RequestBody @Valid createWorkoutRequest: CreateWorkoutRequest
    ): WorkoutResponseDTO {
        return workoutService.updateWorkoutV2(workoutUUID, createWorkoutRequest)
    }

    @ApiOperation("Delete a workout on a given day with the matching title")
    @RequestMapping(value = ["xc/workout/delete"], method = [RequestMethod.DELETE])
    fun deleteWorkoutsV2(

            @ApiParam("uuid")
            @RequestParam(value = "uuid", required = true) uuid: String,

            ): WorkoutResponseDTO? {

        return workoutService.deleteWorkoutV2(uuid)
    }

    @ApiOperation("Get the plan for the given workout")
    @RequestMapping(value = ["xc/workout/plan"], method = [RequestMethod.GET])
    fun getWorkoutPlanV2(

            @ApiParam("uuid")
            @RequestParam(value = "uuid", required = true) uuid: String,

            ): WorkoutPlanResponseV2 {

        return workoutService.getWorkoutPlanV2(uuid)
    }

}