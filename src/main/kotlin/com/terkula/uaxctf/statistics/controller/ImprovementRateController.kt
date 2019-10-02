package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statistics.response.ImprovementRateResponse
import com.terkula.uaxctf.statistics.response.MeetProgressionResponse
import com.terkula.uaxctf.statistics.service.ImprovementRateService
import com.terkula.uaxctf.statistics.service.MeetProgressionService
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.sql.Date
import javax.validation.constraints.Pattern

@RestController
class ImprovementRateController (@field:Autowired
                                 internal var improvementRateService: ImprovementRateService) {

    @ApiOperation("Returns the average week over week improvement for the given runner in the given season, defaulting to current season")
    @RequestMapping(value = ["xc/improvementRate/forRunner"], method = [RequestMethod.GET])
    fun getImprovementRateForRunner(
            @ApiParam("filters results for runners containing the given name/partial name")
            @RequestParam(value = "filter.runner", required = true) runnerName: String,
            @ApiParam("filters results for performances in a given season'")
            @RequestParam ("filter.season", defaultValue = "") filterSeason: String,
            @ApiParam("filters out results for a meet matching/partially the given input. Useful to remove outlier meets")
            @RequestParam("filter.excludeMeet", defaultValue = "") excludedMeet: String) : ImprovementRateResponse {

        val startDate: Date
        val endDate: Date

        if (filterSeason.isEmpty()) {
            startDate = Date.valueOf(MeetPerformanceController.CURRENT_YEAR + "-01-01")
            endDate = Date.valueOf((MeetPerformanceController.CURRENT_YEAR) + "-12-31")
        } else {
            startDate = Date.valueOf("$filterSeason-01-01")
            endDate = Date.valueOf("$filterSeason-12-31")
        }

        return ImprovementRateResponse(improvementRateService.getImprovementRateForRunner(runnerName, startDate, endDate, excludedMeet))

    }

    @ApiOperation("Returns the average week over week improvement for each runner in the given season")
    @RequestMapping(value = ["xc/improvementRate/forAllRunners"], method = [RequestMethod.GET])
    fun getImprovementRatesForAllRunner(
            @ApiParam("filters results for performances in a given season, defaulting to current season")
            @RequestParam("filter.season", defaultValue = "") filterSeason: String,
            @ApiParam("filters out results for a meet matching/partially the given input. Useful to remove outlier meets")
            @RequestParam("filter.excludeMeet", defaultValue = "") excludedMeet: String) : ImprovementRateResponse {

        val startDate: Date
        val endDate: Date

        if (filterSeason.isEmpty()) {
            startDate = Date.valueOf(MeetPerformanceController.CURRENT_YEAR + "-01-01")
            endDate = Date.valueOf((MeetPerformanceController.CURRENT_YEAR) + "-12-31")
        } else {
            startDate = Date.valueOf("$filterSeason-01-01")
            endDate = Date.valueOf("$filterSeason-12-31")
        }

        return ImprovementRateResponse(improvementRateService.getImprovementRateForAllRunners(startDate, endDate, excludedMeet))

    }


}