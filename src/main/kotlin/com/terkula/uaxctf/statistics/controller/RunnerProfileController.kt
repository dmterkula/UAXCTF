package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statistics.dto.RunnerProfileDTOV2
import com.terkula.uaxctf.statistics.response.RunnerProfileResponse
import com.terkula.uaxctf.statistics.service.RunnerProfileService
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class RunnerProfileController (@field:Autowired
                               internal val runnerProfileService: RunnerProfileService) {

    @ApiOperation("Returns PRs for all runners in the the given date constraints")
    @RequestMapping(value = ["xc/runnerProfile/"], method = [RequestMethod.GET])
    fun getAllPRsByYear(
            @ApiParam("Specifies the runner whose profile is to be returned")
            @RequestParam(value = "filter.name", required = true) name: String): RunnerProfileResponse {

        return RunnerProfileResponse(runnerProfileService.buildRunnerProfile(name))

    }

    @ApiOperation("Returns PRs for all runners in the the given date constraints")
    @RequestMapping(value = ["xc/runnerProfileV2/"], method = [RequestMethod.GET])
    fun getRunnerProfile(
            @ApiParam("Specifies the runner whose profile is to be returned")
            @RequestParam(value = "runnerId", required = true) runnerId: Int,


            @ApiParam("Specifies season")
            @RequestParam(value = "season", required = true) season: String
            ): RunnerProfileDTOV2 {

        return runnerProfileService.buildRunnerProfileV2(runnerId, season)

    }

}