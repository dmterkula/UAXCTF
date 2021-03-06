package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statisitcs.model.TimeTrial
import com.terkula.uaxctf.statistics.dto.SeasonBestToTimeTrialDTO
import com.terkula.uaxctf.statistics.dto.TimeTrialImprovementDTO
import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.statistics.repository.TimeTrialRepository
import com.terkula.uaxctf.statistics.dto.StatisticalComparisonDTO
import com.terkula.uaxctf.statistics.dto.TStatDTO
import com.terkula.uaxctf.statistics.response.TTestResponse
import com.terkula.uaxctf.util.*
import org.apache.commons.math3.stat.inference.TestUtils
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

    fun runTTestBetweenPreviousSBsToTimeTrial(
            startDate1: Date,
            endDate1: Date,
            startDate2: Date,
            endDate2: Date,
            adjustForMeetDistance: Boolean
    ): TTestResponse {

        val statistcalDistributionYear1 = getPreviousSBsToTimeTrialDifference(startDate1, endDate1, adjustForMeetDistance)
        val statistcalDistributionYear2 = getPreviousSBsToTimeTrialDifference(startDate2, endDate2, adjustForMeetDistance)

        val tStat = TStatDTO("2 sample T test for mean difference in previous SB times to following year's Time Trial",
                TestUtils.tTest(getMeanDifferenceBetweenPreviousSBsAndTimeTrial(startDate1, endDate1, adjustForMeetDistance).toDoubleArray(),
                getMeanDifferenceBetweenPreviousSBsAndTimeTrial(startDate2, endDate2, adjustForMeetDistance).toDoubleArray())
                        .round(4))

        return TTestResponse(listOf(statistcalDistributionYear1), listOf(statistcalDistributionYear2), listOf(tStat))

    }

    fun runTTestBetweenTimeTrialAndSB(
            startDate1: Date,
            endDate1: Date,
            startDate2: Date,
            endDate2: Date,
            adjustForMeetDistance: Boolean
    ): TTestResponse {

        val statistcalDistributionYear1 = getTimeTrialToSBDifference(startDate1, endDate1, adjustForMeetDistance)
        val statistcalDistributionYear2 = getTimeTrialToSBDifference(startDate2, endDate2, adjustForMeetDistance)

        val tStat = TStatDTO("2 sample T test for mean difference in Time Trial times to end of season best times",
                TestUtils.tTest(getMeanDifferenceBetweenTimeTrialAndSB(startDate1, endDate1, adjustForMeetDistance).toDoubleArray(),
                        getMeanDifferenceBetweenTimeTrialAndSB(startDate2, endDate2, adjustForMeetDistance).toDoubleArray())
                        .round(4))

        return TTestResponse(listOf(statistcalDistributionYear1), listOf(statistcalDistributionYear2), listOf(tStat))

    }

    fun getPreviousSBsToTimeTrialDifference(startDate: Date, endDate: Date, adjustForMeetDistance: Boolean): StatisticalComparisonDTO {

        return StatisticalComparisonDTO.from(startDate.getYearString(),
                getMeanDifferenceBetweenPreviousSBsAndTimeTrial(startDate, endDate, adjustForMeetDistance),
                "time",
                2)
    }

    fun getTimeTrialToSBDifference(startDate: Date, endDate: Date, adjustForMeetDistance: Boolean): StatisticalComparisonDTO {

        return StatisticalComparisonDTO.from(startDate.getYearString(),
                getMeanDifferenceBetweenTimeTrialAndSB(startDate, endDate, adjustForMeetDistance),
                "time",
                2)
    }

    fun getMeanDifferenceBetweenPreviousSBsAndTimeTrial(startDate: Date, endDate: Date, adjustForMeetDistance: Boolean): List<Double> {
        val adjustedTimeTrialResults = getAllAdjustedTimeTrials(startDate, endDate)

        val runners = adjustedTimeTrialResults
                .map { it.runnerId to runnerRepository.findById(it.runnerId).get() }
                .toMap()

        val seasonBests = seasonBestService.getAllSeasonBests(startDate.subtractYear(1), endDate.subtractYear(1), adjustForMeetDistance)
                .filter { it.runner.id in runners.keys }
                .map { runners[it.runner.id]!!.id to it }
                .toMap()

        return adjustedTimeTrialResults
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
    }

    fun getMeanDifferenceBetweenTimeTrialAndSB(startDate: Date, endDate: Date, adjustForMeetDistance: Boolean): List<Double> {
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
                    SeasonBestToTimeTrialDTO(it.value!!, it.key.time)
                }
                .map {
                    it.getDifference()
                }
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