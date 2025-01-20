package com.terkula.uaxctf.training.controller

import com.terkula.uaxctf.training.response.trainingdashboard.TrainingDashboardResponse
import com.terkula.uaxctf.training.service.TrainingDashboardService
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.sql.Date

@RestController
@Validated
class TrainingDashboardController(val dashboardService: TrainingDashboardService) {

    @ApiOperation("Returns the planned training run between the given dates")
    @RequestMapping(value = ["training-dashboard/team"], method = [RequestMethod.GET])
    fun getBaseTrainingPerformance(

        @ApiParam("season, xc or track")
        @RequestParam(value = "season", required = true) season: String,
        @ApiParam("Earliest Date to look for")
        @RequestParam(value = "startDate", required = true) startDate: Date,

        @ApiParam("Latest date to look for")
        @RequestParam(value = "endDate", required = true) endDate: Date,

        @ApiParam("team")
        @RequestParam(value = "team", required = false) team: String? = "UA",
        ): TrainingDashboardResponse  {

        return dashboardService.getTrainingDashboard(season, startDate, endDate, team)
    }
}