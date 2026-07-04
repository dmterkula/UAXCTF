package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statistics.response.CoachInsightsResponse
import com.terkula.uaxctf.statistics.service.CoachInsightsService
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.sql.Date
import java.time.LocalDate

@RestController
class CoachInsightsController(val coachInsightsService: CoachInsightsService) {

    @RequestMapping(value = ["xc/coach-insights"], method = [RequestMethod.GET])
    fun getCoachInsights(
        @RequestParam(value = "season", required = true) season: String,
        @RequestParam(value = "date", required = false) dateParam: Date?,
        @RequestParam(value = "team", required = false) team: String?
    ): CoachInsightsResponse {
        val referenceDate = dateParam ?: Date.valueOf(LocalDate.now())
        return coachInsightsService.getInsights(season, referenceDate, team)
    }
}
