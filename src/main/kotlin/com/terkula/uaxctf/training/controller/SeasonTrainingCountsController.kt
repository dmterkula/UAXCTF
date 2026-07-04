package com.terkula.uaxctf.training.controller

import com.terkula.uaxctf.training.response.runnerseasontrainingcount.RunnerSeasonTrainingCount
import com.terkula.uaxctf.training.service.SummerTrainingAwardService
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

    @RequestMapping(value = ["/season-training-counts/for-all-runners"], method = [RequestMethod.GET])
    fun getRunnersTrainingCounts(

            @RequestParam(value = "season", required = true) season: String,

            @RequestParam(value = "year", required = true) year: String,

            @RequestParam(value = "team", required = true) team: String,

            ): List<RunnerSeasonTrainingCount> {

        return summerTrainingAwardService.getAllRunnersSeasonTrainingStatus(season, year, team)
    }

    @RequestMapping(value = ["/season-training-counts/for-runner"], method = [RequestMethod.GET])
    fun getARunnersTrainingCounts(

            @RequestParam(value = "runnerId", required = true) runnerId: Int,

            @RequestParam(value = "season", required = true) season: String,

            @RequestParam(value = "year", required = true) year: String,

            @RequestParam(value = "team", required = true) team: String,

            ): RunnerSeasonTrainingCount? {

        return summerTrainingAwardService.getSeasonTrainingCountsForRunner(runnerId, season, year, team)
    }

}