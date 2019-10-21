package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statistics.exception.UnsupportedAPIOperationException
import com.terkula.uaxctf.statistics.response.PRResponse
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
            @RequestParam(value = "filter.name", required = true) name: String): String {


        runnerProfileService.buildRunnerProfile(name)
        return "test"

    }




}