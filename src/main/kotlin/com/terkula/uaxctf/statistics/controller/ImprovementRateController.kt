package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statistics.response.ImprovementRateResponse
import com.terkula.uaxctf.statistics.response.MeetProgressionResponse
import com.terkula.uaxctf.statistics.service.ImprovementRateService
import com.terkula.uaxctf.statistics.service.MeetProgressionService
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
    @RequestMapping(value = ["/improvementRate/forRunner"], method = [RequestMethod.GET])
    fun getImprovementRateForRunner(
            @RequestParam(value = "filter.runner", required = true) runnerName: String,
            @RequestParam ("filter.season", defaultValue = "") filterSeason: String,
            @RequestParam("filter.excludeMeet", defaultValue = "") excludedMeet: String) : ImprovementRateResponse {

        var startDate: Date
        var endDate: Date

        if (filterSeason.isEmpty()) {
            startDate = Date.valueOf(MeetPerformanceController.CURRENT_YEAR + "-01-01")
            endDate = Date.valueOf((MeetPerformanceController.CURRENT_YEAR) + "-12-31")
        } else {
            startDate = Date.valueOf("$filterSeason-01-01")
            endDate = Date.valueOf("$filterSeason-12-31")
        }

        return ImprovementRateResponse(improvementRateService.getImprovementRateForRunner(runnerName, startDate, endDate, excludedMeet))

    }

    @RequestMapping(value = ["/improvementRate/forAllRunners"], method = [RequestMethod.GET])
    fun getImprovementRatesForAllRunner(
            @RequestParam("filter.season", defaultValue = "") filterSeason: String,
            @RequestParam("filter.excludeMeet", defaultValue = "") excludedMeet: String) : ImprovementRateResponse {

        var startDate: Date
        var endDate: Date

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