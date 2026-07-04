package com.terkula.uaxctf.training.controller

import com.terkula.uaxctf.training.model.DateRangeRunSummaryDTO
import com.terkula.uaxctf.training.model.TrainingComment
import com.terkula.uaxctf.training.response.CoachTrainingRunInsightsDTO
import com.terkula.uaxctf.training.model.TrainingRunResults
import com.terkula.uaxctf.training.request.CreateRunnersTrainingRunRequest
import com.terkula.uaxctf.training.request.CreateTrainingRunRequest
import com.terkula.uaxctf.training.request.crosstraining.AddTrainingCommentReactionRequest
import com.terkula.uaxctf.training.request.crosstraining.CreateCommentRequest
import com.terkula.uaxctf.training.request.crosstraining.RemoveTrainingCommentReactionRequest
import com.terkula.uaxctf.statistics.response.ReactionSummary
import com.terkula.uaxctf.training.response.RankedRunnerDistanceRunDTO
import com.terkula.uaxctf.training.response.RunnersTrainingRunResponse
import com.terkula.uaxctf.training.response.TrainingRunDTO
import com.terkula.uaxctf.training.response.TrainingRunResponse
import com.terkula.uaxctf.training.response.crosstraining.CrossTrainingRecordResponse
import com.terkula.uaxctf.training.service.TrainingRunsService
import com.terkula.uaxctf.util.TimeUtilities
import com.terkula.uaxctf.util.subtractDays
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

    @RequestMapping(value = ["xc/training-run/get"], method = [RequestMethod.GET])
    fun getTrainingRuns(

        @RequestParam(value = "startDate", required = true) startDate: Date,

        @RequestParam(value = "endDate", required = true) endDate: Date,

        ): TrainingRunResponse {

        return trainingRunsService.getTrainingRuns(startDate, endDate)
    }

    @RequestMapping(value = ["xc/training-run/by-uuid/get"], method = [RequestMethod.GET])
    fun getTrainingRun(

            @RequestParam(value = "uuid", required = true) uuid: String,

            ): TrainingRunDTO? {

        return trainingRunsService.getTrainingRun(uuid)
    }


    @RequestMapping(value = ["xc/training-run/create"], method = [RequestMethod.POST])
    fun createTrainingRun(
        @RequestBody @Valid createTrainingRunRequest: CreateTrainingRunRequest
    ): TrainingRunResponse {
        return trainingRunsService.createTrainingRun(createTrainingRunRequest)
    }

    @RequestMapping(value = ["xc/training-run/update"], method = [RequestMethod.PUT])
    fun updateTrainingRun(
        @RequestBody @Valid createTrainingRunRequest: CreateTrainingRunRequest
    ): TrainingRunResponse {
        return trainingRunsService.updateTrainingRun(createTrainingRunRequest)
    }


    @RequestMapping(value = ["xc/training-run/delete"], method = [RequestMethod.DELETE])
    fun deleteTrainingRun(
        @RequestParam(value = "uuid", required = true) uuid: String,

    ): TrainingRunResponse {

        return trainingRunsService.deleteTrainingRun(uuid)
    }


    @RequestMapping(value = ["xc/runners-training-run-result"], method = [RequestMethod.GET])
    fun getARunnersTrainingRuns(

            @RequestParam(value = "runnerId", required = true) runnerId: Int,

            @RequestParam(value = "trainingRunUUID", required = true) trainingRunUUID: String,

            ): RunnersTrainingRunResponse {

        return trainingRunsService.getRunnersTrainingRun(runnerId, trainingRunUUID)
    }

    @RequestMapping(value = ["xc/runner-training-run-results"], method = [RequestMethod.GET])
    fun getAllARunnersTrainingRun(

            @RequestParam(value = "runnerId", required = true) runnerId: Int,

            @RequestParam(value = "startDate", required = true) startDate: Date,

            @RequestParam(value = "endDate", required = true) endDate: Date,

            ): TrainingRunResults {

        return trainingRunsService.getARunnersTrainingRunsWithinDates(runnerId, startDate, endDate)
    }

    @RequestMapping(value = ["xc/all-runners-training-run-results/get"], method = [RequestMethod.GET])
    fun getAllRunnerTrainingRunsForAGivenPractice(

            @RequestParam(value = "trainingRunUUID", required = true) trainingRunUUID: String,

            ): RunnersTrainingRunResponse {

        return trainingRunsService.getAllRunnersTrainingRun(trainingRunUUID)
    }

    @RequestMapping(value = ["xc/runners-training-run/create"], method = [RequestMethod.POST])
    fun createRunnersTrainingRunResult(
            @RequestBody @Valid createRunnersTrainingRunRequest: CreateRunnersTrainingRunRequest
    ): RunnersTrainingRunResponse {
        return trainingRunsService.createRunnersTrainingRun(createRunnersTrainingRunRequest)
    }

    @RequestMapping(value = ["xc/runners-training-run/update"], method = [RequestMethod.PUT])
    fun updateRunnersTrainingRunResult(
            @RequestBody @Valid createRunnersTrainingRunRequest: CreateRunnersTrainingRunRequest
    ): RunnersTrainingRunResponse {
        return trainingRunsService.updateRunnersTrainingRun(createRunnersTrainingRunRequest)
    }

    @RequestMapping(value = ["xc/runners-training-run/delete"], method = [RequestMethod.DELETE])
    fun deleteRunnersTrainingRun(
            @RequestParam(value = "uuid", required = true) uuid: String,

            ): RunnersTrainingRunResponse {

        return trainingRunsService.deleteRunnersTrainingRun(uuid)
    }


    @RequestMapping(value = ["xc/training-run/runner-distance-run"], method = [RequestMethod.GET])
    fun getARunnersSeasonDistance(

            @RequestParam(value = "season", required = true) season: String,

            @RequestParam(value = "runnerId", required = true) runnerId: Int,

            ): List<RankedRunnerDistanceRunDTO> {

        return trainingRunsService.getAllTrainingMilesRunForARunner(runnerId, season)
    }

    @RequestMapping(value = ["xc/training-run/runner-summary"], method = [RequestMethod.GET])
    fun getARunnersWeeklySummary(

            @RequestParam(value = "season", required = true) season: String,

            @RequestParam(value = "runnerId", required = true) runnerId: Int,

            @Pattern(regexp = "daily|weekly|monthly", message = "only supported timeFrame values are 'daily', 'weekly', or 'monthly'")
            @RequestParam(value = "timeFrame", required = false) timeFrame: String? = "weekly",
            @RequestParam(value = "includeWarmUps", required = false) includeWarmUps: Boolean = false,
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

    @RequestMapping(value = ["xc/training-run/coach-insights"], method = [RequestMethod.GET])
    fun getCoachTrainingRunInsights(
        @RequestParam(value = "team", required = true) team: String,
        @RequestParam(value = "season", required = true) season: String,
        @RequestParam(value = "seasonType", required = false, defaultValue = "xc") seasonType: String,
        @RequestParam(value = "startDate", required = true) startDate: Date,
        @RequestParam(value = "endDate", required = true) endDate: Date,
        @RequestParam(value = "runnerIds", required = false) runnerIds: String?,
    ): CoachTrainingRunInsightsDTO {
        val parsedRunnerIds = runnerIds
            ?.split(",")
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?: emptyList()
        return trainingRunsService.getCoachTrainingRunInsights(team, season, seasonType, parsedRunnerIds, startDate, endDate)
    }

    @RequestMapping(value = ["/training-comment/create"], method = [RequestMethod.POST])
    fun createTrainingComment(
            @RequestBody @Valid createCommentRequest: CreateCommentRequest
    ): TrainingComment {
        return trainingRunsService.createComment(createCommentRequest)
    }

    @RequestMapping(value = ["/training-comment/reactions/add"], method = [RequestMethod.POST])
    fun addCommentReaction(
        @RequestBody @Valid request: AddTrainingCommentReactionRequest
    ): List<ReactionSummary> {
        return trainingRunsService.addCommentReaction(request)
    }

    @RequestMapping(value = ["/training-comment/reactions/remove"], method = [RequestMethod.POST])
    fun removeCommentReaction(
        @RequestBody @Valid request: RemoveTrainingCommentReactionRequest
    ): List<ReactionSummary> {
        return trainingRunsService.removeCommentReaction(request)
    }

    @RequestMapping(value = ["xc/runners-training-run"], method = [RequestMethod.GET])
    fun getTrainingRunRecord(
            @RequestParam(value = "uuid", required = true) uuid: String,
    ): RunnersTrainingRunResponse? {

        return trainingRunsService.getRunnersTrainingRunRecord(uuid)
    }

    @RequestMapping(value = ["xc/training-run/reattribute-points"], method = [RequestMethod.POST])
    fun reattributePoints(
            @RequestParam(value = "trainingRunUuid", required = true) trainingRunUuid: String,
            @RequestParam(value = "fromSeason", required = true) fromSeason: String,
            @RequestParam(value = "toSeason", required = true) toSeason: String,
    ): String {
        return trainingRunsService.forceReattributePoints(trainingRunUuid, fromSeason, toSeason)
    }
}