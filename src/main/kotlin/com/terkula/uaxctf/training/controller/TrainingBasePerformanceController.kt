package com.terkula.uaxctf.training.controller

import com.terkula.uaxctf.training.model.trainingbase.BasePercentPerformanceAndPacePerMile
import com.terkula.uaxctf.training.request.CreateBaseTrainingPerformanceRequest
import com.terkula.uaxctf.training.request.CreateRunnerBaseTrainingPercentageRequest
import com.terkula.uaxctf.training.response.*
import com.terkula.uaxctf.training.service.TrainingBasePerformanceService
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import org.threeten.bp.Instant
import java.sql.Date
import javax.validation.Valid

@RestController
@Validated
class TrainingBasePerformanceController(
        val trainingBasePerformanceService: TrainingBasePerformanceService
) {

    @RequestMapping(value = ["training/base-performances"], method = [RequestMethod.GET])
    fun getBaseTrainingPerformance(

        @RequestParam(value = "runnerId", required = true) runnerId: Int,

        @RequestParam(value = "season", required = true) season: String,

        @RequestParam(value = "year", required = true)  year: String,

    ): TrainingBasePerformanceResponse {

        return trainingBasePerformanceService.getRunnersTrainingBasePerformance(runnerId, season, year)
    }

    @RequestMapping(value = ["training/base-performances/create"], method = [RequestMethod.POST])
    fun createBaseTrainingPerformance(
        @RequestBody @Valid createBaseTrainingPerformanceRequest: CreateBaseTrainingPerformanceRequest
    ): TrainingBasePerformanceResponse {

        return trainingBasePerformanceService.createOrUpdateTrainingBasePerformance(createBaseTrainingPerformanceRequest)
    }

    @RequestMapping(value = ["training/base-performances/createManually"], method = [RequestMethod.POST])
    fun createBaseTrainingPerformanceManually(
        @RequestParam(value = "runnerName", required = true) runnerName: String,

        @RequestParam(value = "season", required = true) season: String,

        @RequestParam(value = "year", required = true)  year: String,

        @RequestParam(value = "event", required = true)  event: String,

        @RequestParam(value = "time", required = true)  time: String,

        @RequestParam(value = "time", required = true)  uuid: String,

    ): TrainingBasePerformanceResponse? {

        return trainingBasePerformanceService.createOrUpdateTrainingBasePerformanceManually()
    }

    @RequestMapping(value = ["training/base-performances/getAll"], method = [RequestMethod.GET])
    fun getBaseTrainingPerformances(
            @RequestParam(value = "season", required = true) season: String,

            @RequestParam(value = "team", required = false) team: String?,

            @RequestParam(value = "year", required = true)  year: String,
    ): List<TrainingBasePerformanceResponse> {
        var teamValue: String = "UA"
        if (team != null) {
            teamValue = team
        }

        return trainingBasePerformanceService.getBaseTrainingPerformances(season, year, teamValue)
    }

    @RequestMapping(value = ["training/base-paces/for-runner"], method = [RequestMethod.GET])
    fun getBaseTrainingPacesForRunner(
            @RequestParam(value = "type", required = true) type: String,

            @RequestParam(value = "season", required = true) season: String,

            @RequestParam(value = "year", required = true)  year: String,

            @RequestParam(value = "runnerId", required = true) runnerId: Int
    ): TrainingRunPaceRange? {

        return trainingBasePerformanceService.getRunnersBaseTrainingPaces(type, season, year, runnerId)
    }

    @RequestMapping(value = ["training/base-paces/for-runner/managementView"], method = [RequestMethod.GET])
    fun getBaseTrainingPacesForRunnerForView(

            @RequestParam(value = "event", required = true) event: String,

            @RequestParam(value = "season", required = true) season: String,

            @RequestParam(value = "year", required = true)  year: String,

            @RequestParam(value = "runnerId", required = true) runnerId: Int
    ): RunnersTrainingPacePercentLabels {

        return trainingBasePerformanceService.getRunnersBaseTrainingPacesForView(event, season, year, runnerId)
    }

    @RequestMapping(value = ["training/base-paces/for-all-runners/managementView"], method = [RequestMethod.GET])
    fun getBaseTrainingPacesForAllRunnersForView(

            @RequestParam(value = "event", required = true) event: String,

            @RequestParam(value = "season", required = true) season: String,

            @RequestParam(value = "year", required = true)  year: String,

    ): List<RunnersTrainingPacePercentLabels> {
        return trainingBasePerformanceService.getAllRunnersBaseTrainingPacesForView(event, season, year)
    }

    @RequestMapping(value = ["training/base-paces/for-all-runners"], method = [RequestMethod.GET])
    fun getBaseTrainingPacesForAllRunners(
            @RequestParam(value = "type", required = true) type: String,

            @RequestParam(value = "season", required = true) season: String,

            @RequestParam(value = "year", required = true)  year: String,

            @RequestParam(value = "date", required = true)  date: String?,

    ): List<RunnersTrainingRunPaceRange> {

        val dateToUse =
        if (date == null) {
            Date(Instant.now().toEpochMilli())
        } else {
            Date.valueOf(date)
        }

        return trainingBasePerformanceService.getAllRunnersBaseTrainingPaces(type, season, year, dateToUse)
    }

    @RequestMapping(value = ["training/base-paces/custom-percentages"], method = [RequestMethod.GET])
    fun getPersonalBaseTrainingPercentagesForAllRunners(
            @RequestParam(value = "season", required = true) season: String,

            @RequestParam(value = "year", required = true)  year: String,

            @RequestParam(value = "team", required = true)  team: String,

            ): List<RunnerBaseTrainingPercentageResponse> {

        return trainingBasePerformanceService.getAllRunnersBaseTrainingPercentages(season, year, team)
    }

    @RequestMapping(value = ["training/base-paces/custom-percentages/create"], method = [RequestMethod.POST])
    fun setPersonalBaseTrainingPercentagesForRunner(
            @RequestBody @Valid createRunnerBaseTrainingPercentage: CreateRunnerBaseTrainingPercentageRequest
        ): RunnersTrainingPacePercentLabels {

        return trainingBasePerformanceService.createRunnersBaseTrainingPercentage(createRunnerBaseTrainingPercentage)
    }

    @RequestMapping(value = ["training/base-paces/custom-percentages/runner-reset"], method = [RequestMethod.POST])
    fun reSetPersonalBaseTrainingPercentagesForRunnerToEventDefault(
            @RequestBody @Valid createRunnerBaseTrainingPercentage: CreateRunnerBaseTrainingPercentageRequest
    ): RunnersTrainingPacePercentLabels {

        return trainingBasePerformanceService.resetRunnersBaseTrainingPercentagesToDefault(createRunnerBaseTrainingPercentage)
    }


    @RequestMapping(value = ["training/base-paces/labels/paceTypes"], method = [RequestMethod.GET])
    fun getTrainingPaceLabels(
            @RequestParam(value = "season", required = true) season: String,


            @RequestParam(value = "year", required = true)  year: String,

            @RequestParam(value = "event", required = true)  event: String,

            ): List<String> {

        return trainingBasePerformanceService.getPaceTypes(season, year, event)
    }

    @RequestMapping(value = ["training/base-paces/runners-assigned-pace"], method = [RequestMethod.GET])
    fun getRunnersAssignedPace(
            @RequestParam(value = "season", required = true) season: String,

            @RequestParam(value = "year", required = true) year: String,

            @RequestParam(value = "event", required = true) event: String,

            @RequestParam(value = "runnerId", required = true) runnerId: Int,

            @RequestParam(value = "paceType", required = true) paceType: String,

            @RequestParam(value = "paceName", required = true) paceName: String,

            ): BasePercentPerformanceAndPacePerMile {

        return trainingBasePerformanceService.getCurrentBasePercentAndPacePerMile(event, season, year, paceType, paceName, runnerId)
    }

    @RequestMapping(value = ["training/base-paces/runners-assigned-pace/calculate"], method = [RequestMethod.GET])
    fun calculateRunnersAssignedPace(
            @RequestParam(value = "season", required = true) season: String,

            @RequestParam(value = "year", required = true) year: String,

            @RequestParam(value = "event", required = true) event: String,

            @RequestParam(value = "runnerId", required = true) runnerId: Int,

            @RequestParam(value = "percent", required = true) percent: Int

            ): BasePercentPerformanceAndPacePerMile {

        return trainingBasePerformanceService.calculatePacePerMileForNewPercent(event, season, year, percent, runnerId)
    }

    @RequestMapping(value = ["training/base-paces/labels/paceNames"], method = [RequestMethod.GET])
    fun getTrainingPaceNames(
            @RequestParam(value = "season", required = true) season: String,


            @RequestParam(value = "year", required = true)  year: String,

            @RequestParam(value = "event", required = true)  event: String,

            ): List<String> {

        return trainingBasePerformanceService.getPaceNames(season, year, event)
    }

    // Historical tracking endpoints

    @RequestMapping(value = ["training/base-performances/history"], method = [RequestMethod.GET])
    fun getBaseTrainingPerformanceHistory(
            @RequestParam(value = "runnerId", required = true) runnerId: Int,

            @RequestParam(value = "season", required = true) season: String,

            @RequestParam(value = "year", required = false) year: String?
    ): TrainingBasePerformanceHistoryResponse {
        val runner = trainingBasePerformanceService.runnerRepository.findById(runnerId).get()
        val history = if (year != null) {
            trainingBasePerformanceService.getTrainingBasePerformanceHistory(runnerId, season, year)
        } else {
            trainingBasePerformanceService.getTrainingBasePerformanceHistory(runnerId, season)
        }
        return TrainingBasePerformanceHistoryResponse(runner, history)
    }

    @RequestMapping(value = ["training/base-performances/at-date"], method = [RequestMethod.GET])
    fun getBaseTrainingPerformanceAtDate(
            @RequestParam(value = "runnerId", required = true) runnerId: Int,

            @RequestParam(value = "season", required = true) season: String,

            @RequestParam(value = "date", required = true) dateString: String,

            @RequestParam(value = "event", required = false) event: String?,

            @RequestParam(value = "year", required = false) year: String?
    ): TrainingBasePerformanceResponse {
        val date = Date.valueOf(dateString)
        val runner = trainingBasePerformanceService.runnerRepository.findById(runnerId).get()

        val basePerformance = when {
            event != null && year != null -> trainingBasePerformanceService.getTrainingBasePerformanceAtDate(runnerId, event, season, year, date)
            event != null -> trainingBasePerformanceService.getTrainingBasePerformanceAtDate(runnerId, event, season, date)
            else -> trainingBasePerformanceService.getTrainingBasePerformanceAtDate(runnerId, season, date)
        }

        return TrainingBasePerformanceResponse(runner, basePerformance)
    }


}