package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.training.model.TrainingComment
import com.terkula.uaxctf.training.request.lifting.*
import com.terkula.uaxctf.training.response.lifting.*
import com.terkula.uaxctf.training.service.LiftingService
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

    @RequestMapping(value = ["exercises"], method = [RequestMethod.GET])
    fun getExercises(
        @RequestParam(value = "team", required = true) team: String
    ): ExercisesResponse {
        return liftingService.getExercises(team)
    }

    @RequestMapping(value = ["exercises"], method = [RequestMethod.POST])
    fun createExercise(
        @RequestBody @Valid request: CreateExerciseRequest
    ): ExerciseDTO {
        return liftingService.createExercise(request)
    }

    @RequestMapping(value = ["exercises/{uuid}"], method = [RequestMethod.PUT])
    fun updateExercise(
        @PathVariable uuid: String,
        @RequestBody @Valid request: UpdateExerciseRequest
    ): ExerciseDTO {
        require(uuid == request.uuid) { "UUID mismatch" }
        return liftingService.updateExercise(request)
    }

    @RequestMapping(value = ["exercises/{uuid}"], method = [RequestMethod.DELETE])
    fun deleteExercise(
        @PathVariable uuid: String
    ) {
        liftingService.deleteExercise(uuid)
    }

    // ===== Activity Endpoints =====

    @RequestMapping(value = ["lifting/activities"], method = [RequestMethod.GET])
    fun getLiftingActivities(
        @RequestParam(value = "startDate", required = true) startDate: Date,
        @RequestParam(value = "endDate", required = true) endDate: Date,
        @RequestParam(value = "team", required = false) team: String?
    ): LiftingActivitiesResponse {
        return liftingService.getLiftingActivities(startDate, endDate, team)
    }

    @RequestMapping(value = ["lifting/activities"], method = [RequestMethod.POST])
    fun createLiftingActivity(
        @RequestBody @Valid request: CreateLiftingActivityRequest
    ): LiftingActivityDTO {
return liftingService.createLiftingActivity(request)
    }

    @RequestMapping(value = ["lifting/activities/{uuid}"], method = [RequestMethod.PUT])
    fun updateLiftingActivity(
        @PathVariable uuid: String,
        @RequestBody @Valid request: UpdateLiftingActivityRequest
    ): LiftingActivityDTO {
        require(uuid == request.uuid) { "UUID mismatch" }
        return liftingService.updateLiftingActivity(request)
    }

    @RequestMapping(value = ["lifting/activities/{uuid}"], method = [RequestMethod.DELETE])
    fun deleteLiftingActivity(
        @PathVariable uuid: String
    ) {
        liftingService.deleteLiftingActivity(uuid)
    }

    // ===== Record Endpoints =====

    @RequestMapping(value = ["lifting/records"], method = [RequestMethod.GET])
    fun getLiftingRecords(
        @RequestParam(value = "activityUuid", required = false) activityUuid: String?,
        @RequestParam(value = "runnerId", required = false) runnerId: Int?,
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

    @RequestMapping(value = ["lifting/records"], method = [RequestMethod.POST])
    fun createLiftingRecord(
        @RequestBody @Valid request: CreateLiftingRecordRequest
    ): LiftingRecordResponse {
        return liftingService.createLiftingRecord(request)
    }

    @RequestMapping(value = ["lifting/records/{uuid}"], method = [RequestMethod.PUT])
    fun updateLiftingRecord(
        @PathVariable uuid: String,
        @RequestBody @Valid request: UpdateLiftingRecordRequest
    ): LiftingRecordResponse {
        require(uuid == request.uuid) { "UUID mismatch" }
        return liftingService.updateLiftingRecord(request)
    }

    // ===== PR Endpoints =====

    @RequestMapping(value = ["lifting/prs"], method = [RequestMethod.GET])
    fun getRunnerPRs(
        @RequestParam(value = "runnerId", required = true) runnerId: Int,
        @RequestParam(value = "exerciseUuid", required = false) exerciseUuid: String?,
        @RequestParam(value = "startDate", required = false) startDate: java.sql.Timestamp?,
        @RequestParam(value = "endDate", required = false) endDate: java.sql.Timestamp?
    ): LiftingPRsResponse {
        return liftingService.getRunnerPRs(runnerId, exerciseUuid, startDate, endDate)
    }

    @RequestMapping(value = ["lifting/exercise-history"], method = [RequestMethod.GET])
    fun getExerciseHistory(
        @RequestParam(value = "runnerId", required = true) runnerId: Int,
        @RequestParam(value = "exerciseUuid", required = true) exerciseUuid: String,
        @RequestParam(value = "startDate", required = true) startDate: java.sql.Timestamp,
        @RequestParam(value = "endDate", required = true) endDate: java.sql.Timestamp
    ): ExerciseHistoryResponse {
        return liftingService.getExerciseHistory(runnerId, exerciseUuid, startDate, endDate)
    }

    @RequestMapping(value = ["lifting/runner/{runnerId}/activities"], method = [RequestMethod.GET])
    fun getRunnerLiftingActivities(
        @PathVariable runnerId: Int,
        @RequestParam(value = "startDate", required = true) startDate: java.sql.Timestamp,
        @RequestParam(value = "endDate", required = true) endDate: java.sql.Timestamp
    ): RunnerLiftingActivitiesResponse {
        return liftingService.getRunnerLiftingActivities(runnerId, startDate, endDate)
    }

    // ===== Comment Endpoints =====

    @RequestMapping(value = ["lifting/comments"], method = [RequestMethod.POST])
    fun createComment(
        @RequestBody @Valid request: CreateLiftingCommentRequest
    ): TrainingComment {
        return liftingService.createComment(request)
    }
}
