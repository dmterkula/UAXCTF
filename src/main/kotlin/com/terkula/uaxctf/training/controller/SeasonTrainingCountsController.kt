package com.terkula.uaxctf.training.controller

import com.terkula.uaxctf.training.response.runnerseasontrainingcount.RunnerSeasonTrainingCount
import com.terkula.uaxctf.training.service.SummerTrainingAwardService
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@Validated
class SeasonTrainingCountsController(
        val summerTrainingAwardService: SummerTrainingAwardService
) {

    @ApiOperation("Returns Runners current training counts")
    @RequestMapping(value = ["/season-training-counts/for-all-runners"], method = [RequestMethod.GET])
    fun getRunnersTrainingCounts(

            @ApiParam("season")
            @RequestParam(value = "season", required = true) season: String,

            @ApiParam("year")
            @RequestParam(value = "year", required = true) year: String,

            @ApiParam("team")
            @RequestParam(value = "team", required = true) team: String,

            ): List<RunnerSeasonTrainingCount> {

        return summerTrainingAwardService.getAllRunnersSeasonTrainingStatus(season, year, team)
    }

    @ApiOperation("Returns Runners current training counts")
    @RequestMapping(value = ["/season-training-counts/for-runner"], method = [RequestMethod.GET])
    fun getARunnersTrainingCounts(

            @ApiParam("runnerId")
            @RequestParam(value = "runnerId", required = true) runnerId: Int,

            @ApiParam("season")
            @RequestParam(value = "season", required = true) season: String,

            @ApiParam("year")
            @RequestParam(value = "year", required = true) year: String,

            @ApiParam("team")
            @RequestParam(value = "team", required = true) team: String,

            ): RunnerSeasonTrainingCount? {

        return summerTrainingAwardService.getSeasonTrainingCountsForRunner(runnerId, season, year, team)
    }

}