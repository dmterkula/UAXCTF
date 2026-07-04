package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statistics.response.TeamReadinessResponse
import com.terkula.uaxctf.statistics.service.ReadinessService
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.sql.Date
import java.time.LocalDate

@RestController
class ReadinessController(val readinessService: ReadinessService) {

    @RequestMapping(value = ["xc/readiness"], method = [RequestMethod.GET])
    fun getTeamReadiness(
        @RequestParam(value = "season", required = true) season: String,
        @RequestParam(value = "date", required = false) dateParam: Date?,
        @RequestParam(value = "team", required = false) team: String?
    ): TeamReadinessResponse {
        val referenceDate = dateParam ?: Date.valueOf(LocalDate.now())
        return readinessService.getTeamReadiness(season, referenceDate, team)
    }
}
