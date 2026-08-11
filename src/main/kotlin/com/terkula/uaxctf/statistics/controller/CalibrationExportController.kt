package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statistics.repository.MeetPerformanceRepository
import com.terkula.uaxctf.statistics.repository.MeetRepository
import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.statistics.repository.TimeTrialRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Read-only export of raw race data in the exact shape the xc-coach-sim calibration harness consumes
 * (`engine/calibration/types.ts` → RawExport). One flat pull of every meet performance (plus preseason
 * time trials) tagged with the runner's graduating class and the season (year of the meet), so the
 * harness can fit the improvement / field / retention distributions off real times.
 *
 * Distances are intentionally omitted — the harness assumes 5k (the DB stores no distance). Nothing is
 * mutated. Hit it and save the body as export.json:
 *
 *   GET xc/calibration/export            → all runners
 *   GET xc/calibration/export?team=girls → one squad
 */
@RestController
class CalibrationExportController(
    @field:Autowired private val meetRepository: MeetRepository,
    @field:Autowired private val meetPerformanceRepository: MeetPerformanceRepository,
    @field:Autowired private val runnerRepository: RunnerRepository,
    @field:Autowired private val timeTrialRepository: TimeTrialRepository
) {

    data class RawPerformanceDTO(
        val runnerId: Int,
        val name: String,
        val graduatingClass: String,
        val season: Int,
        val date: String,
        val meetName: String,
        val time: String
    )

    data class RawTimeTrialDTO(
        val runnerId: Int,
        val season: Int,
        val time: String
    )

    data class RawExportResponse(
        val program: String,
        val performances: List<RawPerformanceDTO>,
        val timeTrials: List<RawTimeTrialDTO>
    )

    @RequestMapping(value = ["xc/calibration/export"], method = [RequestMethod.GET])
    fun export(@RequestParam(value = "team", required = false) team: String?): RawExportResponse {

        val runners = runnerRepository.findAll().associateBy { it.id }
        val meets = meetRepository.findAll().associateBy { it.id }

        val performances = meetPerformanceRepository.findAll()
            .mapNotNull { perf ->
                val meet = meets[perf.meetId] ?: return@mapNotNull null
                val runner = runners[perf.runnerId] ?: return@mapNotNull null
                if (team != null && !runner.team.equals(team, ignoreCase = true)) return@mapNotNull null
                RawPerformanceDTO(
                    runnerId = perf.runnerId,
                    name = runner.name,
                    graduatingClass = runner.graduatingClass,
                    season = meet.date.toLocalDate().year,
                    date = meet.date.toLocalDate().toString(), // yyyy-MM-dd
                    meetName = meet.name,
                    time = perf.time
                )
            }

        val timeTrials = timeTrialRepository.findAll()
            .mapNotNull { tt ->
                val runner = runners[tt.runnerId] ?: return@mapNotNull null
                if (team != null && !runner.team.equals(team, ignoreCase = true)) return@mapNotNull null
                val seasonYear = tt.season.trim().toIntOrNull() ?: return@mapNotNull null
                RawTimeTrialDTO(runnerId = tt.runnerId, season = seasonYear, time = tt.time)
            }

        return RawExportResponse(
            program = team ?: "UAXCTF",
            performances = performances,
            timeTrials = timeTrials
        )
    }
}
