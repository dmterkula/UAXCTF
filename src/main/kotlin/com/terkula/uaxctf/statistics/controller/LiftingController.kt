package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.training.model.TrainingComment
import com.terkula.uaxctf.training.request.lifting.*
import com.terkula.uaxctf.training.response.lifting.*
import com.terkula.uaxctf.training.service.LiftingService
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.sql.Date
import javax.validation.Valid

@RestController
@Validated
@RequestMapping("/api")
class LiftingController(
    private val liftingService: LiftingService
) {

    // ===== Exercise Endpoints =====

    @ApiOperation("Get all exercises for a team (global + team-specific)")
    @RequestMapping(value = ["exercises"], method = [RequestMethod.GET])
    fun getExercises(
        @ApiParam("Team code")
        @RequestParam(value = "team", required = true) team: String
    ): ExercisesResponse {
        return liftingService.getExercises(team)
    }

    @ApiOperation("Create a new exercise")
    @RequestMapping(value = ["exercises"], method = [RequestMethod.POST])
    fun createExercise(
        @RequestBody @Valid request: CreateExerciseRequest
    ): ExerciseDTO {
        return liftingService.createExercise(request)
    }

    @ApiOperation("Update an existing exercise")
    @RequestMapping(value = ["exercises/{uuid}"], method = [RequestMethod.PUT])
    fun updateExercise(
        @PathVariable uuid: String,
        @RequestBody @Valid request: UpdateExerciseRequest
    ): ExerciseDTO {
        require(uuid == request.uuid) { "UUID mismatch" }
        return liftingService.updateExercise(request)
    }

    @ApiOperation("Delete an exercise")
    @RequestMapping(value = ["exercises/{uuid}"], method = [RequestMethod.DELETE])
    fun deleteExercise(
        @PathVariable uuid: String
    ) {
        liftingService.deleteExercise(uuid)
    }

    // ===== Activity Endpoints =====

    @ApiOperation("Get lifting activities within date range")
    @RequestMapping(value = ["lifting/activities"], method = [RequestMethod.GET])
    fun getLiftingActivities(
        @ApiParam("Start date")
        @RequestParam(value = "startDate", required = true) startDate: Date,
        @ApiParam("End date")
        @RequestParam(value = "endDate", required = true) endDate: Date,
        @ApiParam("Team filter (optional)")
        @RequestParam(value = "team", required = false) team: String?
    ): LiftingActivitiesResponse {
        return liftingService.getLiftingActivities(startDate, endDate, team)
    }

    @ApiOperation("Create a new lifting activity")
    @RequestMapping(value = ["lifting/activities"], method = [RequestMethod.POST])
    fun createLiftingActivity(
        @RequestBody @Valid request: CreateLiftingActivityRequest
    ): LiftingActivityDTO {
return liftingService.createLiftingActivity(request)
    }

    @ApiOperation("Update a lifting activity")
    @RequestMapping(value = ["lifting/activities/{uuid}"], method = [RequestMethod.PUT])
    fun updateLiftingActivity(
        @PathVariable uuid: String,
        @RequestBody @Valid request: UpdateLiftingActivityRequest
    ): LiftingActivityDTO {
        require(uuid == request.uuid) { "UUID mismatch" }
        return liftingService.updateLiftingActivity(request)
    }

    @ApiOperation("Delete a lifting activity")
    @RequestMapping(value = ["lifting/activities/{uuid}"], method = [RequestMethod.DELETE])
    fun deleteLiftingActivity(
        @PathVariable uuid: String
    ) {
        liftingService.deleteLiftingActivity(uuid)
    }

    // ===== Record Endpoints =====

    @ApiOperation("Get lifting records (supports multiple query modes)")
    @RequestMapping(value = ["lifting/records"], method = [RequestMethod.GET])
    fun getLiftingRecords(
        @ApiParam("Activity UUID (returns all records for this activity)")
        @RequestParam(value = "activityUuid", required = false) activityUuid: String?,
        @ApiParam("Runner ID (combined with activityUuid returns specific runner's record)")
        @RequestParam(value = "runnerId", required = false) runnerId: Int?,
        @ApiParam("Record UUID (returns single record)")
        @RequestParam(value = "uuid", required = false) uuid: String?
    ): Any {
        return when {
            uuid != null -> {
                liftingService.getLiftingRecord(uuid)
                    ?: throw RuntimeException("Record not found")
            }
            activityUuid != null && runnerId != null -> {
                liftingService.getRunnerLiftingRecord(runnerId, activityUuid)
                    ?: throw RuntimeException("Record not found")
            }
            activityUuid != null -> {
                liftingService.getLiftingRecordsForActivity(activityUuid)
            }
            else -> throw IllegalArgumentException("Must provide activityUuid or uuid")
        }
    }

    @ApiOperation("Create a lifting record")
    @RequestMapping(value = ["lifting/records"], method = [RequestMethod.POST])
    fun createLiftingRecord(
        @RequestBody @Valid request: CreateLiftingRecordRequest
    ): LiftingRecordResponse {
        return liftingService.createLiftingRecord(request)
    }

    @ApiOperation("Update a lifting record")
    @RequestMapping(value = ["lifting/records/{uuid}"], method = [RequestMethod.PUT])
    fun updateLiftingRecord(
        @PathVariable uuid: String,
        @RequestBody @Valid request: UpdateLiftingRecordRequest
    ): LiftingRecordResponse {
        require(uuid == request.uuid) { "UUID mismatch" }
        return liftingService.updateLiftingRecord(request)
    }

    // ===== PR Endpoints =====

    @ApiOperation("Get PRs for a runner (optionally time-bound)")
    @RequestMapping(value = ["lifting/prs"], method = [RequestMethod.GET])
    fun getRunnerPRs(
        @ApiParam("Runner ID")
        @RequestParam(value = "runnerId", required = true) runnerId: Int,
        @ApiParam("Exercise UUID (optional, filters to specific exercise)")
        @RequestParam(value = "exerciseUuid", required = false) exerciseUuid: String?,
        @ApiParam("Start date (optional, for time-bound PRs)")
        @RequestParam(value = "startDate", required = false) startDate: java.sql.Timestamp?,
        @ApiParam("End date (optional, for time-bound PRs)")
        @RequestParam(value = "endDate", required = false) endDate: java.sql.Timestamp?
    ): LiftingPRsResponse {
        return liftingService.getRunnerPRs(runnerId, exerciseUuid, startDate, endDate)
    }

    @ApiOperation("Get exercise history for a runner within a date range")
    @RequestMapping(value = ["lifting/exercise-history"], method = [RequestMethod.GET])
    fun getExerciseHistory(
        @ApiParam("Runner ID")
        @RequestParam(value = "runnerId", required = true) runnerId: Int,
        @ApiParam("Exercise UUID")
        @RequestParam(value = "exerciseUuid", required = true) exerciseUuid: String,
        @ApiParam("Start date")
        @RequestParam(value = "startDate", required = true) startDate: java.sql.Timestamp,
        @ApiParam("End date")
        @RequestParam(value = "endDate", required = true) endDate: java.sql.Timestamp
    ): ExerciseHistoryResponse {
        return liftingService.getExerciseHistory(runnerId, exerciseUuid, startDate, endDate)
    }

    @ApiOperation("Get all lifting activities for a specific runner within a date range")
    @RequestMapping(value = ["lifting/runner/{runnerId}/activities"], method = [RequestMethod.GET])
    fun getRunnerLiftingActivities(
        @ApiParam("Runner ID")
        @PathVariable runnerId: Int,
        @ApiParam("Start date")
        @RequestParam(value = "startDate", required = true) startDate: java.sql.Timestamp,
        @ApiParam("End date")
        @RequestParam(value = "endDate", required = true) endDate: java.sql.Timestamp
    ): RunnerLiftingActivitiesResponse {
        return liftingService.getRunnerLiftingActivities(runnerId, startDate, endDate)
    }

    // ===== Comment Endpoints =====

    @ApiOperation("Create a comment on a lifting record")
    @RequestMapping(value = ["lifting/comments"], method = [RequestMethod.POST])
    fun createComment(
        @RequestBody @Valid request: CreateLiftingCommentRequest
    ): TrainingComment {
        return liftingService.createComment(request)
    }
}
