package com.terkula.uaxctf.training.controller

import com.terkula.uaxctf.training.response.crosstraining.CrossTrainingResponse
import com.terkula.uaxctf.training.service.CoachDavidIsmsService
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@Validated
class CoachDavidIsmController(
    val coachDavidIsmsService: CoachDavidIsmsService
) {
    @ApiOperation("Returns coach david isms filtered by tagged team")
    @RequestMapping(value = ["/coachDavidIsms"], method = [RequestMethod.GET])
    fun getCrossTrainingActivities(

            @ApiParam("for NU")
            @RequestParam(value = "forNu", required = true) forNu: Boolean,

            @ApiParam("forUa")
            @RequestParam(value = "forUa", required = true) forUa: Boolean,

            ): List<String> {

        return coachDavidIsmsService.getCoachDavidIsms(forNu, forUa)
    }


}