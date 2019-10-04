package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statistics.response.RunnerGoalResponse
import com.terkula.uaxctf.statistics.service.XcGoalService
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class XcGoalController(@field:Autowired
                        internal var xcGoalService: XcGoalService) {

    @ApiOperation("Returns the season 5k goal for the given runner")
    @RequestMapping(value = ["xc/goals/forRunner"], method = [RequestMethod.GET])
    fun getRunnerGoalsByName(
            @ApiParam("Filters results for athlete with the given name. ")
            @RequestParam(value = "filter.runner", required = true) name: String,
            @ApiParam("Filters results for the season in the given year.")
            @RequestParam(value = "filter.season", required = false, defaultValue = "") season: String): RunnerGoalResponse {

        var filterSeason = season

        if (season.isEmpty()) {
            filterSeason = MeetPerformanceController.CURRENT_YEAR
        }

        return RunnerGoalResponse(xcGoalService.getRunnersGoalForSeason(name, filterSeason))

    }

    @ApiOperation("Returns the season 5k goal for all runners in the given year")
    @RequestMapping(value = ["xc/goals/forSeason"], method = [RequestMethod.GET])
    fun getRunnerGoals(
            @ApiParam("Filters results for the season in the given year.")
            @RequestParam(value = "filter.season", required = false, defaultValue = "") season: String): RunnerGoalResponse {

        var filterSeason = season

        if (season.isEmpty()) {
            filterSeason = MeetPerformanceController.CURRENT_YEAR
        }

        return RunnerGoalResponse(xcGoalService.getGoalsForSeason(filterSeason))

    }

}