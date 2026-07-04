package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statistics.dto.RunnerGoalDTO
import com.terkula.uaxctf.statistics.request.GoalsRequest
import com.terkula.uaxctf.statistics.request.UpdateGoalRequest
import com.terkula.uaxctf.statistics.response.MetGoalResponse
import com.terkula.uaxctf.statistics.response.RunnerGoalResponse
import com.terkula.uaxctf.statistics.response.UnMetGoalResponse
import com.terkula.uaxctf.statistics.service.XcGoalService
import com.terkula.uaxctf.util.TimeUtilities
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import java.sql.Date

@RestController
class XcGoalController(@field:Autowired
                        internal var xcGoalService: XcGoalService) {

    @RequestMapping(value = ["xc/goals/forSeason"], method = [RequestMethod.GET])
    fun getRunnerGoals(
            @RequestParam(value = "filter.season", required = false, defaultValue = "") season: String,
            @RequestParam(value = "filter.xcOnly", required = false
            ) xcOnly: Boolean?,
            @RequestParam(value = "filter.trackOnly", required = false)
            trackOnly: Boolean?): RunnerGoalResponse {

        var filterSeason = season

        if (season.isEmpty()) {
            filterSeason = MeetPerformanceController.CURRENT_YEAR
        }

        return RunnerGoalResponse(xcGoalService.getGoalsForSeason(filterSeason, xcOnly, trackOnly))

    }


    @RequestMapping(value = ["xc/goals/newlyMetAtMeet"], method = [RequestMethod.GET])
    fun getRunnerNewlyMetGoalsAtMeet(
            @RequestParam(value = "filter.meet", required = false, defaultValue = "") meetName: String,
            @RequestParam(value = "adjust.forDistance", required = false, defaultValue = "false") adjustForDistance: Boolean = false): MetGoalResponse {

        val startDate = Date.valueOf("${MeetPerformanceController.CURRENT_YEAR}-01-01")
        val endDate = Date.valueOf((MeetPerformanceController.CURRENT_YEAR) + "-12-31")

        return MetGoalResponse(xcGoalService.getRunnerWhoNewlyMetGoalAtMeet(meetName, startDate, endDate, adjustForDistance))

    }

    @RequestMapping(value = ["xc/goals/metThisSeason"], method = [RequestMethod.GET])
    fun getMetRunnerGoalsThisSeason(
            @RequestParam(value = "adjust.forDistance", required = false, defaultValue = "false") adjustForDistance: Boolean = false,
            @RequestParam(value = "filter.season", required = true) season: String
    ): MetGoalResponse {

        val startDate = TimeUtilities.getFirstDayOfGivenYear(season)
        val endDate = TimeUtilities.getLastDayOfGivenYear(season)

        return MetGoalResponse(xcGoalService.getRunnersWhoHaveMetGoal(startDate, endDate, adjustForDistance))

    }

    @RequestMapping(value = ["xc/goals/notMetThisSeason"], method = [RequestMethod.GET])
    fun getNotMetRunnerGoalsThisSeason(
           @RequestParam(value = "adjust.forDistance", required = false, defaultValue = "false") adjustForDistance: Boolean = false,
           @RequestParam(value = "filter.season", required = true) season: String
    ): UnMetGoalResponse {

        val startDate = TimeUtilities.getFirstDayOfGivenYear(season)
        val endDate = TimeUtilities.getLastDayOfGivenYear(season)

        return UnMetGoalResponse(xcGoalService.getRunnersWhoHaveNotMetGoal(startDate, endDate, adjustForDistance))

    }

    @RequestMapping(value = ["xc/goals/createV2"], method = [RequestMethod.POST], consumes=["application/json"])
    fun createGoalsV2(
            @RequestParam(value = "filter.runnerId", required = true) runnerId: Int,
            @RequestParam(value = "filter.season", required = true) season: String,
            @RequestBody createGoalsRequest: GoalsRequest
    ): RunnerGoalDTO {

        return xcGoalService.createRunnersGoalsForSeason(runnerId, season, createGoalsRequest)
    }

    @RequestMapping(value = ["xc/goals/update"], method = [RequestMethod.PUT], consumes=["application/json"])
    fun updateGoal(
            @RequestParam(value = "filter.runner", required = true) name: String,
            @RequestParam(value = "filter.season", required = true) season: String,
            @RequestBody updateGoalRequest: UpdateGoalRequest
    ): RunnerGoalDTO {

        return xcGoalService.updateRunnerGoalForSeason(name, season, updateGoalRequest)
    }

    @RequestMapping(value = ["xc/goals/deleteV2"], method = [RequestMethod.DELETE], consumes=["application/json"])
    fun deleteGoals(
            @RequestParam(value = "filter.runnerId", required = true) runnerId: Int,
            @RequestParam(value = "filter.season", required = true) season: String,
            @RequestBody createGoalsRequest: GoalsRequest
    ): RunnerGoalDTO {

        return xcGoalService.deleteRunnersGoals(runnerId, season, createGoalsRequest)
    }

    @RequestMapping(value = ["xc/goals/getV2"], method = [RequestMethod.GET])
    fun getRunnerGoalsById(
            @RequestParam(value = "filter.runnerId", required = true) runnerId: Int,
            @RequestParam(value = "filter.season", required = false, defaultValue = "") season: String,
            @RequestParam(value = "filter.type", required = false, defaultValue = "xc") type: String = "xc"
    ): RunnerGoalDTO {

        var filterSeason = season

        if (season.isEmpty()) {
            filterSeason = MeetPerformanceController.CURRENT_YEAR
        }

        return xcGoalService.getRunnersGoalForYearAndSeason(runnerId, filterSeason, type)

    }

}