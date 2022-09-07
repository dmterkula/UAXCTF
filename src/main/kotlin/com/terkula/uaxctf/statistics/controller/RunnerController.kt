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
    @RequestMapping(value = ["xc/runnersGivenClass/"], method = [RequestMethod.GET])
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

    @ApiOperation("Returns runners, by oldest grad class, then sorted by name")
    @RequestMapping(value = ["xc/allRunners/"], method = [RequestMethod.GET])
    fun getAllRunnersRegardlessOfClass(

    ): List<Runner> {

        val runners = runnerRepository.findAll().groupBy { it.graduatingClass }.map {
            it.key to it.value.sortedBy { runner -> runner.graduatingClass }
        }
                .sortedByDescending { it.first }
                .map { it.second }
                .flatten()

        return runners
    }

    @ApiOperation("Returns roster of runners for given season sorted by seniors first")
    @RequestMapping(value = ["xc/runners"], method = [RequestMethod.GET])
    fun getRunnersForReason(
            @RequestParam(value = "filter.season", required = false, defaultValue = "")
            season: String

    ): List<Runner> {

        val runners = runnerRepository.findAll()
                .filter { it.graduatingClass.toInt() > season.toInt() && it.graduatingClass.toInt() <= season.toInt() + 4 }
                .groupBy { it.graduatingClass }.map {
            it.key to it.value.sortedBy { runner -> runner.name }
        }
                .sortedBy { it.first }
                .map { it.second }
                .flatten()

        return runners
    }
}