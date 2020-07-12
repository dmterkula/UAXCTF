package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.util.TimeUtilities
import com.terkula.uaxctf.util.getYearString
import io.swagger.annotations.ApiOperation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class RunnerController(@field:Autowired val runnerRepository: RunnerRepository) {

    @ApiOperation("Returns runners in the the given class constraints, if no filter.class value is provided," +
            "returns all runners with a grad class greater than current year")
    @RequestMapping(value = ["xc/runners/"], method = [RequestMethod.GET])
    fun getAllRunners(
            @RequestParam(value = "filter.class", required = false, defaultValue = "")
            classYear: String
    ): List<Runner> {

        val currentYear = TimeUtilities.getFirstDayOfYear().getYearString()

        return if (classYear.isNotEmpty()) {
            runnerRepository.findByGraduatingClass(classYear)
        } else {
            runnerRepository.findByGraduatingClassGreaterThan(currentYear)
        }
    }
}