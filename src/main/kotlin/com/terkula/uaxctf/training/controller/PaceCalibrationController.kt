package com.terkula.uaxctf.training.controller

import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.training.response.calibration.PaceCalibrationResponse
import com.terkula.uaxctf.training.response.calibration.RunnerPaceCalibration
import com.terkula.uaxctf.training.service.PaceCalibrationService
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.sql.Date
import java.time.LocalDate

@RestController
class PaceCalibrationController(
    private val paceCalibrationService: PaceCalibrationService,
    private val runnerRepository: RunnerRepository
) {

    @RequestMapping(value = ["xc/pace-calibration"], method = [RequestMethod.GET])
    fun getPaceCalibration(
        @RequestParam(value = "season", required = true) season: String,
        @RequestParam(value = "year", required = true) year: String,
        @RequestParam(value = "team", required = false) team: String?,
        @RequestParam(value = "runnerId", required = false) runnerId: Int?,
        @RequestParam(value = "startDate", required = false) startDateParam: Date?,
        @RequestParam(value = "endDate", required = false) endDateParam: Date?
    ): PaceCalibrationResponse {

        val endDate = endDateParam ?: Date.valueOf(LocalDate.now())
        val startDate = startDateParam ?: Date.valueOf(LocalDate.now().minusDays(28))

        val runners = if (runnerId != null) {
            listOfNotNull(runnerRepository.findById(runnerId).orElse(null))
        } else {
            val active = runnerRepository.findByGraduatingClassGreaterThan(year)
            if (team != null) active.filter { it.team == team } else active
        }

        val calibrations: List<RunnerPaceCalibration> = runners.map { runner ->
            paceCalibrationService.calibrateRunner(runner, season, year, startDate, endDate, team)
        }

        return PaceCalibrationResponse(
            calibrations.sortedWith(
                compareByDescending<RunnerPaceCalibration> { it.confidence }
                    .thenBy { it.runner.name }
            )
        )
    }
}
