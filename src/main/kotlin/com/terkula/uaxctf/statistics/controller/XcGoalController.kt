package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statistics.dto.RunnerGoalDTO
import com.terkula.uaxctf.statistics.request.GoalsRequest
import com.terkula.uaxctf.statistics.request.UpdateGoalRequest
import com.terkula.uaxctf.statistics.response.MetGoalResponse
import com.terkula.uaxctf.statistics.response.RunnerGoalResponse
import com.terkula.uaxctf.statistics.response.UnMetGoalResponse
import com.terkula.uaxctf.statistics.service.XcGoalService
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import java.sql.Date

@RestController
class XcGoalController(@field:Autowired
                        internal var xcGoalService: XcGoalService) {

    @ApiOperation("Returns the season 5k goal for the given runner")
    @RequestMapping(value = ["xc/goals/forRunner"], method = [RequestMethod.GET])
    fun getRunnerGoalsByName(
            @ApiParam("Filters results for athlete with the given name. ")
            @RequestParam(value = "filter.runner", required = true) name: String,
            @ApiParam("Filters results for the season in the given year.")
            @RequestParam(value = "filter.season", required = false, defaultValue = "") season: String): RunnerGoalDTO {

        var filterSeason = season

        if (season.isEmpty()) {
            filterSeason = MeetPerformanceController.CURRENT_YEAR
        }

        return xcGoalService.getRunnersGoalForSeason(name, filterSeason)

    }

    @ApiOperation("Returns the season 5k goal for all runners in the given year")
    @RequestMapping(value = ["xc/goals/forSeason"], method = [RequestMethod.GET])
    fun getRunnerGoals(
            @ApiParam("Filters results for the season in the given year.")
            @RequestParam(value = "filter.season", required = false, defaultValue = "") season: String
            ): RunnerGoalResponse {

        var filterSeason = season

        if (season.isEmpty()) {
            filterSeason = MeetPerformanceController.CURRENT_YEAR
        }

        return RunnerGoalResponse(xcGoalService.getGoalsForSeason(filterSeason))

    }


    @ApiOperation("Returns runners who met their 5k goal at the given meet")
    @RequestMapping(value = ["xc/goals/newlyMetAtMeet"], method = [RequestMethod.GET])
    fun getRunnerNewlyMetGoalsAtMeet(
            @ApiParam("Specify the name of the last meet")
            @RequestParam(value = "filter.meet", required = false, defaultValue = "") meetName: String,
            @ApiParam("Adjusts seasons bests for true distance of the meet if value passed is true")
            @RequestParam(value = "adjust.forDistance", required = false, defaultValue = "false") adjustForDistance: Boolean = false): MetGoalResponse {

        val startDate = Date.valueOf("${MeetPerformanceController.CURRENT_YEAR}-01-01")
        val endDate = Date.valueOf((MeetPerformanceController.CURRENT_YEAR) + "-12-31")

        return MetGoalResponse(xcGoalService.getRunnerWhoNewlyMetGoalAtMeet(meetName, startDate, endDate, adjustForDistance))

    }

    @ApiOperation("Returns runners who have met their goal this season")
    @RequestMapping(value = ["xc/goals/metThisSeason"], method = [RequestMethod.GET])
    fun getRunnerGoalsThisSeason(
            @ApiParam("Adjusts seasons bests for true distance of the meet if value passed is true")
            @RequestParam(value = "adjust.forDistance", required = false, defaultValue = "false") adjustForDistance: Boolean = false
    ): MetGoalResponse {

        val startDate = Date.valueOf("${MeetPerformanceController.CURRENT_YEAR}-01-01")
        val endDate = Date.valueOf((MeetPerformanceController.CURRENT_YEAR) + "-12-31")

        return MetGoalResponse(xcGoalService.getRunnersWhoHaveMetGoal(startDate, endDate, adjustForDistance))

    }

    @ApiOperation("Returns runners who have not met their goal this season")
    @RequestMapping(value = ["xc/goals/notMetThisSeason"], method = [RequestMethod.GET])
    fun getNotMetRunnerGoalsThisSeason(@ApiParam("Adjusts seasons bests for true distance of the meet if value passed is true")
                                       @RequestParam(value = "adjust.forDistance", required = false, defaultValue = "false") adjustForDistance: Boolean = false): UnMetGoalResponse {

        val startDate = Date.valueOf("${MeetPerformanceController.CURRENT_YEAR}-01-01")
        val endDate = Date.valueOf((MeetPerformanceController.CURRENT_YEAR) + "-12-31")

        return UnMetGoalResponse(xcGoalService.getRunnersWhoHaveNotMetGoal(startDate, endDate, adjustForDistance))

    }

    @ApiOperation("Creates a time goal for the given runner in the given season")
    @RequestMapping(value = ["xc/goals/create"], method = [RequestMethod.POST], consumes=["application/json"])
    fun createGoals(
            @ApiParam("Create the goal for the given runner ")
            @RequestParam(value = "filter.runner", required = true) name: String,
            @ApiParam("Filters results for the season in the given year.")
            @RequestParam(value = "filter.season", required = true) season: String,
            @RequestBody createGoalsRequest: GoalsRequest
            ): RunnerGoalDTO {

        return xcGoalService.createRunnersGoalsForSeason(name, season, createGoalsRequest)
    }

    @ApiOperation("Creates a time goal for the given runner in the given season")
    @RequestMapping(value = ["xc/goals/update"], method = [RequestMethod.PUT], consumes=["application/json"])
    fun updateGoal(
            @ApiParam("Create the goal for the given runner ")
            @RequestParam(value = "filter.runner", required = true) name: String,
            @ApiParam("Filters results for the season in the given year.")
            @RequestParam(value = "filter.season", required = true) season: String,
            @RequestBody updateGoalRequest: UpdateGoalRequest
    ): RunnerGoalDTO {

        return xcGoalService.updateRunnerGoalForSeason(name, season, updateGoalRequest)
    }

    @ApiOperation("Creates a time goal for the given runner in the given season")
    @RequestMapping(value = ["xc/goals/delete"], method = [RequestMethod.DELETE], consumes=["application/json"])
    fun deleteGoals(
            @ApiParam("Create the goal for the given runner ")
            @RequestParam(value = "filter.runner", required = true) name: String,
            @ApiParam("Filters results for the season in the given year.")
            @RequestParam(value = "filter.season", required = true) season: String,
            @RequestBody createGoalsRequest: GoalsRequest
    ): RunnerGoalDTO {

        return xcGoalService.deleteRunnersGoals(name, season, createGoalsRequest)
    }

}