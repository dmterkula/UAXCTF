package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.statistics.request.CreateRunnerRequest
import com.terkula.uaxctf.statistics.service.RunnerService
import com.terkula.uaxctf.util.TimeUtilities
import com.terkula.uaxctf.util.getYearString
import org.springframework.web.bind.annotation.*

@RestController
class RunnerController(
    val runnerRepository: RunnerRepository,
    val runnerService: RunnerService
) {

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

    @RequestMapping(value = ["xc/runners"], method = [RequestMethod.GET])
    fun getRunnersForSeason(
            @RequestParam(value = "filter.season", required = false, defaultValue = "")
            season: String,
            @RequestParam(value = "filter.active", required = false, defaultValue = "false")
            active: Boolean,
            @RequestParam(value = "filter.xcRoster", required = false, defaultValue = "true")
            xcRoster: Boolean? = true,
            @RequestParam(value = "filter.getAll", required = false)
            getAll: Boolean?,

    ): List<Runner> {

        val useXcRoster = xcRoster ?: true

        if (getAll != null && getAll) {
            return runnerService.getRoster(active, season)
        }

        return if (useXcRoster) {
            runnerService.getXcRoster(active, season)
        } else {
            runnerService.getTrackRoster(active, season)
        }

    }

    @RequestMapping(value = ["xc/runners/byTeam"], method = [RequestMethod.GET])
    fun getRunnersForSeason(
            @RequestParam(value = "filter.team", required = true, defaultValue = "UA")
            team: String
            ): List<Runner> {

        return runnerService.getRunnersByTeam(team)

    }

    @RequestMapping(value = ["xc/runners/create"], method = [RequestMethod.POST])
    fun createRunner(
            @RequestBody createRunnerRequest: CreateRunnerRequest

    ): Runner {
        return runnerService.createRunner(createRunnerRequest)
    }

    @RequestMapping(value = ["xc/runners/update"], method = [RequestMethod.PUT])
    fun update(
            @RequestParam(value = "runnerId", required = true)
            runnerId: Int,
            @RequestBody createRunnerRequest: CreateRunnerRequest
    ): Runner? {
        return runnerService.updateRunner(runnerId, createRunnerRequest)
    }
}