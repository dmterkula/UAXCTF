package com.terkula.uaxctf.training.controller

import com.terkula.uaxctf.training.request.CreateBaseTrainingPerformanceRequest
import com.terkula.uaxctf.training.response.RunnersTrainingRunPaceRange
import com.terkula.uaxctf.training.response.TrainingRunPaceRange
import com.terkula.uaxctf.training.response.TrainingBasePerformanceResponse
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

            @ApiParam("The year")
            @RequestParam(value = "year", required = true)  year: String,
    ): List<TrainingBasePerformanceResponse> {

        return trainingBasePerformanceService.getBaseTrainingPerformances(season, year)
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
    @RequestMapping(value = ["training/base-paces/for-all-runners"], method = [RequestMethod.GET])
    fun getBaseTrainingPacesForAllRunners(
            @ApiParam("type of run (recovery or base building")
            @RequestParam(value = "type", required = true) type: String,

            @ApiParam("track or xc?")
            @RequestParam(value = "season", required = true) season: String,

            @ApiParam("The year")
            @RequestParam(value = "year", required = true)  year: String,

    ): List<RunnersTrainingRunPaceRange> {

        return trainingBasePerformanceService.getAllRunnersBaseTrainingPaces(type, season, year)
    }



}