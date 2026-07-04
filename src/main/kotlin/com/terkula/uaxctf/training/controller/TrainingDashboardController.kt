package com.terkula.uaxctf.training.controller

import com.terkula.uaxctf.training.response.trainingdashboard.IndividualTrainingDashboardResponse
import com.terkula.uaxctf.training.response.trainingdashboard.TrainingDashboardResponse
import com.terkula.uaxctf.training.service.TrainingDashboardService
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.sql.Date

@RestController
@Validated
class TrainingDashboardController(val dashboardService: TrainingDashboardService) {

    @RequestMapping(value = ["training-dashboard/team"], method = [RequestMethod.GET])
    fun getTeamTrainingDashboard(

        @RequestParam(value = "season", required = true) season: String,
        @RequestParam(value = "startDate", required = true) startDate: Date,

        @RequestParam(value = "endDate", required = true) endDate: Date,

        @RequestParam(value = "team", required = false) team: String? = "UA",
        ): TrainingDashboardResponse  {

        return dashboardService.getTeamTrainingDashboard(season, startDate, endDate, team)
    }

    @RequestMapping(value = ["training-dashboard/individual"], method = [RequestMethod.GET])
    fun getIndividualTrainingDashboard(
            @RequestParam(value = "runnerId", required = true) runnerId: Int,

            @RequestParam(value = "startDate", required = true) startDate: Date,

            @RequestParam(value = "endDate", required = true) endDate: Date,

    ): IndividualTrainingDashboardResponse  {

        return dashboardService.getIndividualTrainingDashboard(runnerId = runnerId, startDate, endDate)
    }
}