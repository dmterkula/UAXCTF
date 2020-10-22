package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statisitcs.model.TimeTrial
import com.terkula.uaxctf.statistics.dto.SeasonBestToTimeTrialDTO
import com.terkula.uaxctf.statistics.dto.TimeTrialImprovementDTO
import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.statistics.repository.TimeTrialRepository
import com.terkula.uaxctf.statistics.response.StatisticalComparisonResponse
import com.terkula.uaxctf.util.*
import org.nield.kotlinstatistics.percentile
import org.nield.kotlinstatistics.standardDeviation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.sql.Date

@Service
class TimeTrialProgressionService (@field: Autowired
                                   internal val runnerRepository: RunnerRepository,
                                   @field:Autowired
                                   internal val timeTrialRepository: TimeTrialRepository,
                                   @field: Autowired
                                   internal val seasonBestService: SeasonBestService) {


    fun getRankedProgressionSinceTimeTrial(startDate: Date, endDate: Date, adjustForMeetDistance: Boolean): List<TimeTrialImprovementDTO> {


        val adjustedTimeTrialResults = getAllAdjustedTimeTrials(startDate, endDate)

        val runners = adjustedTimeTrialResults
                .map { it.runnerId to runnerRepository.findById(it.runnerId).get() }
                .toMap()

        val seasonBests = seasonBestService.getAllSeasonBests(startDate, endDate, adjustForMeetDistance)
                .filter { it.runner.id in runners.keys }
                .map { runners[it.runner.id]!!.id to it }
                .toMap()

        return adjustedTimeTrialResults
                .map { it to seasonBests[it.runnerId] }
                .toMap()
                .filter { it.value != null }
                .map {
                     // if seasonBestDTO is null, difference is 0, or filter them out and give no rank.
                    TimeTrialImprovementDTO(0, runners[it.key.runnerId]!!, it.key.time, it.value!!.seasonBest.first().time,
                            (it.value!!.seasonBest.first().time.calculateSecondsFrom() - it.key.time.calculateSecondsFrom()).toMinuteSecondString())
                }.sortedBy { it.improvement.calculateSecondsFrom() }
                .mapIndexed { index, it ->
                    TimeTrialImprovementDTO(index + 1, it.runner, it.adjustedTimeTrial, it.seasonBest, it.improvement) }
    }


    fun getPreviousSBsToTimeTrialDifference(startDate: Date, endDate: Date, adjustForMeetDistance: Boolean): StatisticalComparisonResponse {

        val adjustedTimeTrialResults = getAllAdjustedTimeTrials(startDate, endDate)

        val runners = adjustedTimeTrialResults
                .map { it.runnerId to runnerRepository.findById(it.runnerId).get() }
                .toMap()

        val seasonBests = seasonBestService.getAllSeasonBests(startDate.subtractYear(1), endDate.subtractYear(1), adjustForMeetDistance)
                .filter { it.runner.id in runners.keys }
                .map { runners[it.runner.id]!!.id to it }
                .toMap()

        val previousSeasonBestToTimeTrialDTOs = adjustedTimeTrialResults
                .map { it to seasonBests[it.runnerId] }
                .toMap()
                .filter { it.value != null }
                .map {
                    // if seasonBestDTO is null, difference is 0, or filter them out and give no rank.
                    SeasonBestToTimeTrialDTO(it.value!!, it.key.time)
                }
                .map {
                    it.getDifference()
                }

        return StatisticalComparisonResponse(
                startDate.getYearString(),
                previousSeasonBestToTimeTrialDTOs.standardDeviation().round(2),
                previousSeasonBestToTimeTrialDTOs.percentile(10.0).round(2).toMinuteSecondString(),
                previousSeasonBestToTimeTrialDTOs.percentile(25.0).round(2).toMinuteSecondString(),
                previousSeasonBestToTimeTrialDTOs.percentile(75.0).round(2).toMinuteSecondString(),
                previousSeasonBestToTimeTrialDTOs.percentile(90.0).round(2).toMinuteSecondString(),
                previousSeasonBestToTimeTrialDTOs.average().round(2).toMinuteSecondString()
        )
    }

    fun getAllAdjustedTimeTrials(startDate: Date, endDate: Date): List<TimeTrial> {
        return timeTrialRepository.findBySeason(startDate.getYearString())
                .map {
                    TimeTrial(it.runnerId, scaleTo5k((mile * 3), it.time.calculateSecondsFrom()).toMinuteSecondString(), it.place, it.season)
                }

    }



    companion object {
        val mile: Double = 1609.0
    }
}