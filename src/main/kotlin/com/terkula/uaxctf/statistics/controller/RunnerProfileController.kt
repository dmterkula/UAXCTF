package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statistics.dto.RunnerProfileDTOV2
import com.terkula.uaxctf.statistics.response.RunnerProfileResponse
import com.terkula.uaxctf.statistics.service.RunnerProfileService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class RunnerProfileController (@field:Autowired
                               internal val runnerProfileService: RunnerProfileService) {

    @RequestMapping(value = ["xc/runnerProfile/"], method = [RequestMethod.GET])
    fun getAllPRsByYear(
            @RequestParam(value = "filter.name", required = true) name: String): RunnerProfileResponse {

        return RunnerProfileResponse(runnerProfileService.buildRunnerProfile(name))

    }

    @RequestMapping(value = ["xc/runnerProfileV2/"], method = [RequestMethod.GET])
    fun getRunnerProfile(
            @RequestParam(value = "runnerId", required = true) runnerId: Int,


            @RequestParam(value = "season", required = true) season: String,

            @RequestParam(value = "includeWarmUps", required = false) includeWarmUps: Boolean = false,

            @RequestParam(value = "type", required = false) type: String? = "xc",
            @RequestParam(value = "team", required = false) team: String? = "UA"
            ): RunnerProfileDTOV2 {

        var trackOrXc = "xc"
        if (type != null) {
           trackOrXc = type
        }

        var teamType = "UA"
        if (team != null && team != "UA") {
            teamType = team
        }

        return runnerProfileService.buildRunnerProfileV2(runnerId, season, trackOrXc, teamType, includeWarmUps)

    }

    @RequestMapping(value = ["xc/runnerWordCounter/"], method = [RequestMethod.GET])
    fun getRunnerWords(
            @RequestParam(value = "runnerId", required = true) runnerId: Int,


            @RequestParam(value = "season", required = true) season: String
    ): Int {

        return runnerProfileService.getRunnerWordCount(runnerId, season)

    }

}