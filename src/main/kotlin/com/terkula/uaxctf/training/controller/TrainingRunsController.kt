package com.terkula.uaxctf.training.controller

import com.terkula.uaxctf.training.model.DateRangeRunSummaryDTO
import com.terkula.uaxctf.training.model.TrainingComment
import com.terkula.uaxctf.training.model.TrainingRunResults
import com.terkula.uaxctf.training.request.CreateRunnersTrainingRunRequest
import com.terkula.uaxctf.training.request.CreateTrainingRunRequest
import com.terkula.uaxctf.training.request.crosstraining.CreateCommentRequest
import com.terkula.uaxctf.training.response.RankedRunnerDistanceRunDTO
import com.terkula.uaxctf.training.response.RunnersTrainingRunResponse
import com.terkula.uaxctf.training.response.TrainingRunDTO
import com.terkula.uaxctf.training.response.TrainingRunResponse
import com.terkula.uaxctf.training.response.crosstraining.CrossTrainingRecordResponse
import com.terkula.uaxctf.training.service.TrainingRunsService
import com.terkula.uaxctf.util.TimeUtilities
import com.terkula.uaxctf.util.subtractDays
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.sql.Date
import javax.validation.Valid
import javax.validation.constraints.Pattern

@RestController
@Validated
class TrainingRunsController(
    val trainingRunsService: TrainingRunsService
) {

    @ApiOperation("Returns the planned training run between the given dates")
    @RequestMapping(value = ["xc/training-run/get"], method = [RequestMethod.GET])
    fun getTrainingRuns(

        @ApiParam("Earliest Date to look for")
        @RequestParam(value = "startDate", required = true) startDate: Date,

        @ApiParam("Latest date to look for")
        @RequestParam(value = "endDate", required = true) endDate: Date,

        ): TrainingRunResponse {

        return trainingRunsService.getTrainingRuns(startDate, endDate)
    }

    @ApiOperation("Returns the planned training run between the given dates")
    @RequestMapping(value = ["xc/training-run/by-uuid/get"], method = [RequestMethod.GET])
    fun getTrainingRun(

            @ApiParam("training run uuid")
            @RequestParam(value = "uuid", required = true) uuid: String,

            ): TrainingRunDTO? {

        return trainingRunsService.getTrainingRun(uuid)
    }


    @ApiOperation("Returns a planned training run")
    @RequestMapping(value = ["xc/training-run/create"], method = [RequestMethod.POST])
    fun createTrainingRun(
        @RequestBody @Valid createTrainingRunRequest: CreateTrainingRunRequest
    ): TrainingRunResponse {
        return trainingRunsService.createTrainingRun(createTrainingRunRequest)
    }

    @ApiOperation("Updates or creates a training-run")
    @RequestMapping(value = ["xc/training-run/update"], method = [RequestMethod.PUT])
    fun updateTrainingRun(
        @RequestBody @Valid createTrainingRunRequest: CreateTrainingRunRequest
    ): TrainingRunResponse {
        return trainingRunsService.updateTrainingRun(createTrainingRunRequest)
    }


    @ApiOperation("Returns the planned training run between the given dates")
    @RequestMapping(value = ["xc/training-run/delete"], method = [RequestMethod.DELETE])
    fun deleteTrainingRun(
        @ApiParam("Training run uuid")
        @RequestParam(value = "uuid", required = true) uuid: String,

    ): TrainingRunResponse {

        return trainingRunsService.deleteTrainingRun(uuid)
    }


    @ApiOperation("Returns a particular runners training runs")
    @RequestMapping(value = ["xc/runners-training-run-result"], method = [RequestMethod.GET])
    fun getARunnersTrainingRuns(

            @ApiParam("runnerId")
            @RequestParam(value = "runnerId", required = true) runnerId: Int,

            @ApiParam("Training run uuid")
            @RequestParam(value = "trainingRunUUID", required = true) trainingRunUUID: String,

            ): RunnersTrainingRunResponse {

        return trainingRunsService.getRunnersTrainingRun(runnerId, trainingRunUUID)
    }

    @ApiOperation("Returns all runners training runs results between dates")
    @RequestMapping(value = ["xc/runner-training-run-results"], method = [RequestMethod.GET])
    fun getAllARunnersTrainingRun(

            @ApiParam("runnerId")
            @RequestParam(value = "runnerId", required = true) runnerId: Int,

            @ApiParam("Earliest Date")
            @RequestParam(value = "startDate", required = true) startDate: Date,

            @ApiParam("Latest Date")
            @RequestParam(value = "endDate", required = true) endDate: Date,

            ): TrainingRunResults {

        return trainingRunsService.getARunnersTrainingRunsWithinDates(runnerId, startDate, endDate)
    }

    @ApiOperation("Returns a particular runners training runs")
    @RequestMapping(value = ["xc/all-runners-training-run-results/get"], method = [RequestMethod.GET])
    fun getAllRunnerTrainingRunsForAGivenPractice(

            @ApiParam("Training run uuid")
            @RequestParam(value = "trainingRunUUID", required = true) trainingRunUUID: String,

            ): RunnersTrainingRunResponse {

        return trainingRunsService.getAllRunnersTrainingRun(trainingRunUUID)
    }

    @ApiOperation("Create a runners training run result")
    @RequestMapping(value = ["xc/runners-training-run/create"], method = [RequestMethod.POST])
    fun createRunnersTrainingRunResult(
            @RequestBody @Valid createRunnersTrainingRunRequest: CreateRunnersTrainingRunRequest
    ): RunnersTrainingRunResponse {
        return trainingRunsService.createRunnersTrainingRun(createRunnersTrainingRunRequest)
    }

    @ApiOperation("Updates or creates a runners training run result")
    @RequestMapping(value = ["xc/runners-training-run/update"], method = [RequestMethod.PUT])
    fun updateRunnersTrainingRunResult(
            @RequestBody @Valid createRunnersTrainingRunRequest: CreateRunnersTrainingRunRequest
    ): RunnersTrainingRunResponse {
        return trainingRunsService.updateRunnersTrainingRun(createRunnersTrainingRunRequest)
    }

    @ApiOperation("Delete the planned training run between the given dates")
    @RequestMapping(value = ["xc/runners-training-run/delete"], method = [RequestMethod.DELETE])
    fun deleteRunnersTrainingRun(
            @ApiParam("Runners Training run uuid")
            @RequestParam(value = "uuid", required = true) uuid: String,

            ): RunnersTrainingRunResponse {

        return trainingRunsService.deleteRunnersTrainingRun(uuid)
    }


    @ApiOperation("Returns total distance run for a given runner in a given season")
    @RequestMapping(value = ["xc/training-run/runner-distance-run"], method = [RequestMethod.GET])
    fun getARunnersSeasonDistance(

            @ApiParam("season")
            @RequestParam(value = "season", required = true) season: String,

            @ApiParam("runnerId")
            @RequestParam(value = "runnerId", required = true) runnerId: Int,

            ): List<RankedRunnerDistanceRunDTO> {

        return trainingRunsService.getAllTrainingMilesRunForARunner(runnerId, season)
    }

    @ApiOperation("Returns total distance run for a given runner in a given season")
    @RequestMapping(value = ["xc/training-run/runner-summary"], method = [RequestMethod.GET])
    fun getARunnersWeeklySummary(

            @ApiParam("season")
            @RequestParam(value = "season", required = true) season: String,

            @ApiParam("runnerId")
            @RequestParam(value = "runnerId", required = true) runnerId: Int,

            @ApiParam("timeFrame")
            @Pattern(regexp = "daily|weekly|monthly", message = "only supported timeFrame values are 'daily', 'weekly', or 'monthly'")
            @RequestParam(value = "timeFrame", required = false) timeFrame: String? = "weekly",
            @RequestParam(value = "includeWarmUps", required = false) includeWarmUps: Boolean = false,
            @ApiParam("type")
            @RequestParam(value = "type", required = false) type: String?,

            ): List<DateRangeRunSummaryDTO> {

        var xcOrTrack = "xc"
        if (type != null) {
            xcOrTrack = type
        }


        var startDate = TimeUtilities.getFirstDayOfGivenYear(season)
        var endDate = TimeUtilities.getLastDayOfGivenYear(season)

        if (xcOrTrack.equals("track", ignoreCase = true)) {
            startDate = TimeUtilities.getFirstDayOfGivenYear(season).subtractDays(90)
            endDate = TimeUtilities.getLastDayOfGivenYear(season).subtractDays(150)
        }

        return if (timeFrame == null || timeFrame == "weekly") {
            trainingRunsService.getTotalDistancePerWeek(startDate, endDate, runnerId, includeWarmUps, xcOrTrack)
        } else if (timeFrame == "monthly") {
            trainingRunsService.getTotalDistancePerMonth(startDate, endDate, runnerId, includeWarmUps, xcOrTrack)
        } else {
            trainingRunsService.getTotalDistancePerDay(startDate, endDate, runnerId, includeWarmUps, xcOrTrack)
        }

    }

    @ApiOperation("Create training run comment")
    @RequestMapping(value = ["/training-comment/create"], method = [RequestMethod.POST])
    fun createTrainingComment(
            @RequestBody @Valid createCommentRequest: CreateCommentRequest
    ): TrainingComment {
        return trainingRunsService.createComment(createCommentRequest)
    }

    @ApiOperation("Returns the planned training run between the given dates")
    @RequestMapping(value = ["xc/runners-training-run"], method = [RequestMethod.GET])
    fun getTrainingRunRecord(
            @ApiParam("runners training run record uuid")
            @RequestParam(value = "uuid", required = true) uuid: String,
    ): RunnersTrainingRunResponse? {

        return trainingRunsService.getRunnersTrainingRunRecord(uuid)
    }
}