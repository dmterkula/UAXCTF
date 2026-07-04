package com.terkula.uaxctf.training.controller

import com.terkula.uaxctf.training.request.CreateSplitsRequest
import com.terkula.uaxctf.training.request.CreateWorkoutRequest
import com.terkula.uaxctf.training.request.LogWorkoutResultsRequest
import com.terkula.uaxctf.training.response.*
import com.terkula.uaxctf.training.service.WorkoutService
import com.terkula.uaxctf.training.service.WorkoutGroupBuilderService
import com.terkula.uaxctf.training.service.WorkoutSplitService
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.sql.Date
import javax.validation.Valid

@RestController
@Validated
class WorkoutController(
    var workoutService: WorkoutService,
    var workoutGroupBuilderService: WorkoutGroupBuilderService,
    var workoutSplitService: WorkoutSplitService

) {

    @RequestMapping(value = ["/workout/groups"], method = [RequestMethod.GET])
    fun buildWorkoutGroups(
            @RequestParam(value = "maxSpread", required = false, defaultValue = "30") maxSpread: Int,
            @RequestParam(value = "minGroupSize", required = true) minGroupSize: Int,
            @RequestParam(value = "maxGroupSize", required = true) maxGroupSize: Int,
            @RequestParam(value = "season", required = true) season: String,
            @RequestParam(value = "fuzzinessFactor", required = true) fuzzinessFactor: Double
           ) {

        workoutGroupBuilderService.buildWorkoutGroups(season, maxSpread, minGroupSize, maxGroupSize, fuzzinessFactor)
    }

    @RequestMapping(value = ["xc/workout/get"], method = [RequestMethod.GET])
    fun getWorkoutsV2(

            @RequestParam(value = "startDate", required = true) startDate: Date,

            @RequestParam(value = "endDate", required = true) endDate: Date,

            ): List<WorkoutResponseDTO> {

        return workoutService.getWorkoutsV2(startDate, endDate)
    }

    @RequestMapping(value = ["xc/workout/create"], method = [RequestMethod.POST])
    fun createWorkoutV2(
           @RequestBody @Valid createWorkoutRequest: CreateWorkoutRequest
    ): WorkoutResponseDTO {
        return workoutService.createWorkoutV2(createWorkoutRequest)
    }

    @RequestMapping(value = ["xc/workout/update"], method = [RequestMethod.PUT])
    fun updateWorkoutV2(
            @RequestParam workoutUUID: String,
            @RequestBody @Valid createWorkoutRequest: CreateWorkoutRequest
    ): WorkoutResponseDTO {
        return workoutService.updateWorkoutV2(workoutUUID, createWorkoutRequest)
    }

    @RequestMapping(value = ["xc/workout/delete"], method = [RequestMethod.DELETE])
    fun deleteWorkoutsV2(

            @RequestParam(value = "uuid", required = true) uuid: String,

            ): WorkoutResponseDTO? {

        return workoutService.deleteWorkoutV2(uuid)
    }

    @RequestMapping(value = ["xc/workout/plan"], method = [RequestMethod.GET])
    fun getWorkoutPlanV2(

            @RequestParam(value = "uuid", required = true) uuid: String,

            ): WorkoutPlanResponseV2 {

        return workoutService.getWorkoutPlanV2(uuid)
    }

    @RequestMapping(value = ["xc/workout/split/create"], method = [RequestMethod.PUT])
    fun createSplits(
            @RequestBody @Valid createSplitsRequest: CreateSplitsRequest
    ): SplitsResponse {
        return workoutSplitService.createSplits(createSplitsRequest)
    }

    @RequestMapping(value = ["xc/workout/split/delete"], method = [RequestMethod.DELETE])
    fun deleteSplits(
            @RequestParam(value = "uuid", required = true) uuid: String,
    ): SplitsResponse {
        return workoutSplitService.delete(uuid)
    }

    @RequestMapping(value = ["xc/workout/splits/get"], method = [RequestMethod.GET])
    fun getSplits(
            @RequestParam(value = "componentUuid", required = true) componentUUID: String,
            @RequestParam(value = "runnerId", required = true) runnerId: Int,

    ): SplitsResponse {
        return workoutSplitService.getSplitsForRunnerAndComponent(runnerId, componentUUID)
    }

    @RequestMapping(value = ["xc/workout/componentResults/get"], method = [RequestMethod.GET])
    fun getComponentResults(
            @RequestParam(value = "componentUuid", required = true) componentUUID: String

            ): ComponentSplitsResponse {
        return workoutSplitService.getWorkoutSplitsForComponent(componentUUID)
    }

    @RequestMapping(value = ["xc/workout/runner-result/put"], method = [RequestMethod.PUT])
    fun logRunnersResults(
        @RequestBody logWorkoutResultsRequest: LogWorkoutResultsRequest
    ): RunnerWorkoutResultResponse {
        return workoutSplitService.logRunnersWorkout(logWorkoutResultsRequest)
    }

    @RequestMapping(value = ["xc/workout/runner-result/get"], method = [RequestMethod.GET])
    fun logRunnersResults(
            @RequestParam(value = "workoutUuid", required = true) workoutUuid: String,

            @RequestParam(value = "runnerId", required = true) runnerId: Int

    ): RunnerWorkoutResultResponse {
        return workoutSplitService.getRunnerWorkoutResults(workoutUuid, runnerId)
    }

    @RequestMapping(value = ["xc/workout/all-runner-results/get"], method = [RequestMethod.GET])
    fun getAllWorkoutResultsRunnersResults(
            @RequestParam(value = "workoutUuid", required = true) workoutUuid: String,

    ): List<RunnerWorkoutResultResponse> {
        return workoutSplitService.getAllResultsForWorkout(workoutUuid)
    }


}