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
            @RequestParam(value = "season", required = true) season: String,

            @RequestParam(value = "includeWarmUps", required = false) includeWarmUps: Boolean = false,

            @RequestParam(value = "type", required = false) type: String? = "xc"
            ): RunnerProfileDTOV2 {

        var trackOrXc = "xc"
        if (type != null) {
           trackOrXc = type
        }

        return runnerProfileService.buildRunnerProfileV2(runnerId, season, trackOrXc, includeWarmUps)

    }

    @ApiOperation("Returns words runner has entered in app for a season")
    @RequestMapping(value = ["xc/runnerWordCounter/"], method = [RequestMethod.GET])
    fun getRunnerWords(
            @ApiParam("Specifies the runner whose profile is to be returned")
            @RequestParam(value = "runnerId", required = true) runnerId: Int,


            @ApiParam("Specifies season")
            @RequestParam(value = "season", required = true) season: String
    ): Int {

        return runnerProfileService.getRunnerWordCount(runnerId, season)

    }

}