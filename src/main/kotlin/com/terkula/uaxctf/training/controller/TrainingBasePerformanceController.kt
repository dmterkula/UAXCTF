package com.terkula.uaxctf.training.controller

import com.terkula.uaxctf.training.model.trainingbase.BasePercentPerformanceAndPacePerMile
import com.terkula.uaxctf.training.request.CreateBaseTrainingPerformanceRequest
import com.terkula.uaxctf.training.request.CreateRunnerBaseTrainingPercentageRequest
import com.terkula.uaxctf.training.response.*
import com.terkula.uaxctf.training.service.TrainingBasePerformanceService
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@RestController
@Validated
class TrainingBasePerformanceController(
        val trainingBasePerformanceService: TrainingBasePerformanceService
) {

    @ApiOperation("Returns the planned training run between the given dates")
    @RequestMapping(value = ["training/base-performances"], method = [RequestMethod.GET])
    fun getBaseTrainingPerformance(

        @ApiParam("The runner to select the base pace for")
        @RequestParam(value = "runnerId", required = true) runnerId: Int,

        @ApiParam("Track or XC?")
        @RequestParam(value = "season", required = true) season: String,

        @ApiParam("The year")
        @RequestParam(value = "year", required = true)  year: String,

    ): TrainingBasePerformanceResponse {

        return trainingBasePerformanceService.getRunnersTrainingBasePerformance(runnerId, season, year)
    }

    @ApiOperation("Create base training performance")
    @RequestMapping(value = ["training/base-performances/create"], method = [RequestMethod.POST])
    fun createBaseTrainingPerformance(
        @RequestBody @Valid createBaseTrainingPerformanceRequest: CreateBaseTrainingPerformanceRequest
    ): TrainingBasePerformanceResponse {

        return trainingBasePerformanceService.createOrUpdateTrainingBasePerformance(createBaseTrainingPerformanceRequest)
    }

    @ApiOperation("Create base training performance manually")
    @RequestMapping(value = ["training/base-performances/createManually"], method = [RequestMethod.POST])
    fun createBaseTrainingPerformanceManually(
        @ApiParam("The runner to select the base pace for")
        @RequestParam(value = "runnerName", required = true) runnerName: String,

        @ApiParam("Track or XC?")
        @RequestParam(value = "season", required = true) season: String,

        @ApiParam("The year")
        @RequestParam(value = "year", required = true)  year: String,

        @ApiParam("The event")
        @RequestParam(value = "event", required = true)  event: String,

        @ApiParam("Time: minutes and seconds")
        @RequestParam(value = "time", required = true)  time: String,

        @ApiParam("Time: minutes and seconds")
        @RequestParam(value = "time", required = true)  uuid: String,

    ): TrainingBasePerformanceResponse? {

        return trainingBasePerformanceService.createOrUpdateTrainingBasePerformanceManually()
    }

    @ApiOperation("Get Base Training Paces")
    @RequestMapping(value = ["training/base-performances/getAll"], method = [RequestMethod.GET])
    fun getBaseTrainingPerformances(
            @ApiParam("Track or XC?")
            @RequestParam(value = "season", required = true) season: String,

            @ApiParam("Team")
            @RequestParam(value = "team", required = false) team: String?,

            @ApiParam("The year")
            @RequestParam(value = "year", required = true)  year: String,
    ): List<TrainingBasePerformanceResponse> {
        var teamValue: String = "UA"
        if (team != null) {
            teamValue = team
        }

        return trainingBasePerformanceService.getBaseTrainingPerformances(season, year, teamValue)
    }

    @ApiOperation("Get Base Training Paces")
    @RequestMapping(value = ["training/base-paces/for-runner"], method = [RequestMethod.GET])
    fun getBaseTrainingPacesForRunner(
            @ApiParam("type of run (recovery or base building")
            @RequestParam(value = "type", required = true) type: String,

            @ApiParam("track or xc?")
            @RequestParam(value = "season", required = true) season: String,

            @ApiParam("The year")
            @RequestParam(value = "year", required = true)  year: String,

            @ApiParam("The runner to select the base pace for")
            @RequestParam(value = "runnerId", required = true) runnerId: Int
    ): TrainingRunPaceRange? {

        return trainingBasePerformanceService.getRunnersBaseTrainingPaces(type, season, year, runnerId)
    }

    @ApiOperation("Get Base Training Paces")
    @RequestMapping(value = ["training/base-paces/for-runner/managementView"], method = [RequestMethod.GET])
    fun getBaseTrainingPacesForRunnerForView(

            @ApiParam("type of run (recovery or base building")
            @RequestParam(value = "event", required = true) event: String,

            @ApiParam("track or xc?")
            @RequestParam(value = "season", required = true) season: String,

            @ApiParam("The year")
            @RequestParam(value = "year", required = true)  year: String,

            @ApiParam("The runner to select the base pace for")
            @RequestParam(value = "runnerId", required = true) runnerId: Int
    ): RunnersTrainingPacePercentLabels {

        return trainingBasePerformanceService.getRunnersBaseTrainingPacesForView(event, season, year, runnerId)
    }

    @ApiOperation("Get All Base Training Paces")
    @RequestMapping(value = ["training/base-paces/for-all-runners/managementView"], method = [RequestMethod.GET])
    fun getBaseTrainingPacesForAllRunnersForView(

            @ApiParam("type of run (recovery or base building")
            @RequestParam(value = "event", required = true) event: String,

            @ApiParam("track or xc?")
            @RequestParam(value = "season", required = true) season: String,

            @ApiParam("The year")
            @RequestParam(value = "year", required = true)  year: String,

    ): List<RunnersTrainingPacePercentLabels> {
        return trainingBasePerformanceService.getAllRunnersBaseTrainingPacesForView(event, season, year)
    }

    @ApiOperation("Get Base Training Paces")
    @RequestMapping(value = ["training/base-paces/for-all-runners"], method = [RequestMethod.GET])
    fun getBaseTrainingPacesForAllRunners(
            @ApiParam("type of run (recovery or base building)")
            @RequestParam(value = "type", required = true) type: String,

            @ApiParam("track or xc?")
            @RequestParam(value = "season", required = true) season: String,

            @ApiParam("The year")
            @RequestParam(value = "year", required = true)  year: String,

    ): List<RunnersTrainingRunPaceRange> {

        return trainingBasePerformanceService.getAllRunnersBaseTrainingPaces(type, season, year)
    }

    @ApiOperation("Get All Runners Personal Pace Percentages")
    @RequestMapping(value = ["training/base-paces/custom-percentages"], method = [RequestMethod.GET])
    fun getPersonalBaseTrainingPercentagesForAllRunners(
            @ApiParam("track or xc?")
            @RequestParam(value = "season", required = true) season: String,

            @ApiParam("The year")
            @RequestParam(value = "year", required = true)  year: String,

            @ApiParam("Team")
            @RequestParam(value = "team", required = true)  team: String,

            ): List<RunnerBaseTrainingPercentageResponse> {

        return trainingBasePerformanceService.getAllRunnersBaseTrainingPercentages(season, year, team)
    }

    @ApiOperation("Set Base Training Percentages For Runner")
    @RequestMapping(value = ["training/base-paces/custom-percentages/create"], method = [RequestMethod.POST])
    fun setPersonalBaseTrainingPercentagesForRunner(
            @RequestBody @Valid createRunnerBaseTrainingPercentage: CreateRunnerBaseTrainingPercentageRequest
        ): RunnersTrainingPacePercentLabels {

        return trainingBasePerformanceService.createRunnersBaseTrainingPercentage(createRunnerBaseTrainingPercentage)
    }

    @ApiOperation("Re-Set Base Training Percentages For Runner to Default")
    @RequestMapping(value = ["training/base-paces/custom-percentages/runner-reset"], method = [RequestMethod.POST])
    fun reSetPersonalBaseTrainingPercentagesForRunnerToEventDefault(
            @RequestBody @Valid createRunnerBaseTrainingPercentage: CreateRunnerBaseTrainingPercentageRequest
    ): RunnersTrainingPacePercentLabels {

        return trainingBasePerformanceService.resetRunnersBaseTrainingPercentagesToDefault(createRunnerBaseTrainingPercentage)
    }


    @ApiOperation("Get All Runners Personal Pace Percentages")
    @RequestMapping(value = ["training/base-paces/labels/paceTypes"], method = [RequestMethod.GET])
    fun getTrainingPaceLabels(
            @ApiParam("track or xc?")
            @RequestParam(value = "season", required = true) season: String,


            @ApiParam("year")
            @RequestParam(value = "year", required = true)  year: String,

            @ApiParam("event")
            @RequestParam(value = "event", required = true)  event: String,

            ): List<String> {

        return trainingBasePerformanceService.getPaceTypes(season, year, event)
    }

    @ApiOperation("Get Runners Training Pace For Pace Type and Pace Name")
    @RequestMapping(value = ["training/base-paces/runners-assigned-pace"], method = [RequestMethod.GET])
    fun getRunnersAssignedPace(
            @ApiParam("track or xc?")
            @RequestParam(value = "season", required = true) season: String,

            @ApiParam("year")
            @RequestParam(value = "year", required = true) year: String,

            @ApiParam("event")
            @RequestParam(value = "event", required = true) event: String,

            @ApiParam("runnerId")
            @RequestParam(value = "runnerId", required = true) runnerId: Int,

            @ApiParam("paceType")
            @RequestParam(value = "paceType", required = true) paceType: String,

            @ApiParam("paceName")
            @RequestParam(value = "paceName", required = true) paceName: String,

            ): BasePercentPerformanceAndPacePerMile {

        return trainingBasePerformanceService.getCurrentBasePercentAndPacePerMile(event, season, year, paceType, paceName, runnerId)
    }

    @ApiOperation("Get Runners Training Pace For Pace Type and Pace Name")
    @RequestMapping(value = ["training/base-paces/runners-assigned-pace/calculate"], method = [RequestMethod.GET])
    fun calculateRunnersAssignedPace(
            @ApiParam("track or xc?")
            @RequestParam(value = "season", required = true) season: String,

            @ApiParam("year")
            @RequestParam(value = "year", required = true) year: String,

            @ApiParam("event")
            @RequestParam(value = "event", required = true) event: String,

            @ApiParam("runnerId")
            @RequestParam(value = "runnerId", required = true) runnerId: Int,

            @ApiParam("percent")
            @RequestParam(value = "percent", required = true) percent: Int

            ): BasePercentPerformanceAndPacePerMile {

        return trainingBasePerformanceService.calculatePacePerMileForNewPercent(event, season, year, percent, runnerId)
    }

    @ApiOperation("Get All Runners Personal Pace Percentages")
    @RequestMapping(value = ["training/base-paces/labels/paceNames"], method = [RequestMethod.GET])
    fun getTrainingPaceNames(
            @ApiParam("track or xc?")
            @RequestParam(value = "season", required = true) season: String,


            @ApiParam("year")
            @RequestParam(value = "year", required = true)  year: String,

            @ApiParam("event")
            @RequestParam(value = "event", required = true)  event: String,

            ): List<String> {

        return trainingBasePerformanceService.getPaceNames(season, year, event)
    }


}