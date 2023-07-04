package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statisitcs.model.TimeTrial
import com.terkula.uaxctf.statistics.dto.*
import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.statistics.repository.TimeTrialRepository
import com.terkula.uaxctf.statistics.response.TTestResponse
import com.terkula.uaxctf.util.*
import org.apache.commons.math3.stat.inference.TestUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.sql.Date

@Service
class TimeTrialService (@field: Autowired
                                   internal val runnerRepository: RunnerRepository,
                        @field:Autowired
                                   internal val timeTrialRepository: TimeTrialRepository,
                        @field: Autowired
                                   internal val seasonBestService: SeasonBestService) {


    fun getTimeTrialResults(startDate: Date, endDate: Date, scaleTo5k: Boolean): List<TimeTrialDTO> {

        var season = startDate.getYearString();

        val timeTrialResults = timeTrialRepository.findBySeason(season)

        val runners = timeTrialResults
                .map { it.runnerId to runnerRepository.findById(it.runnerId).get() }
                .toMap()


        return timeTrialResults.map {
            var time = if (scaleTo5k) {
                it.time.calculateSecondsFrom().scaleTimeTo5k(4827.0).toMinuteSecondString()
            } else {
                it.time
            }
            TimeTrialDTO(runners[it.runnerId]!!, time, it.place, season)}.sortedBy { it.place }

    }

    fun createTimeTrialResult(season: String, runnerId: Int, time: String, place: Int): TimeTrialDTO {

       val runner = runnerRepository.findById(runnerId).get()
       val results = timeTrialRepository.findByRunnerIdAndSeason(runnerId, season)

        return if (results.isEmpty()) {
            // create new result

            val newResult = TimeTrial(runnerId, time, place, season)
            timeTrialRepository.save(newResult)

            TimeTrialDTO(runner, newResult.time, newResult.place, newResult.season)

        } else {
            val updatedRecord = results.first()
            updatedRecord.time = time
            updatedRecord.place = place
            timeTrialRepository.save(updatedRecord)
            TimeTrialDTO(runner, updatedRecord.time, updatedRecord.place, season)
        }

    }

    fun getTimeTrialComparisonsBetweenYearsForSameRunners(startDate: Date, endDate: Date): List<TimeTrialDifferenceDTO> {
        val timeTrialResultsInputYear = timeTrialRepository.findBySeason(startDate.getYearString())

        val timeTrialResultsPreviousYear = timeTrialRepository.findBySeason(startDate.subtractYear(1).getYearString())
                .map {it.runnerId to it}.toMap()

        val runners = timeTrialResultsInputYear
                .map { it.runnerId to runnerRepository.findById(it.runnerId).get() }
                .toMap()

        return timeTrialResultsInputYear.filter { timeTrialResultsPreviousYear[it.runnerId] != null }
                .map { TimeTrialDifferenceDTO(runners[it.runnerId]!!, (it.time.calculateSecondsFrom() - timeTrialResultsPreviousYear[it.runnerId]!!.time.calculateSecondsFrom()).toMinuteSecondString(),
                    listOf(TimeTrialDTO(runners[it.runnerId]!!, it.time, it.place, startDate.getYearString()),
                            TimeTrialDTO(runners[it.runnerId]!!, timeTrialResultsPreviousYear[it.runnerId]!!.time, timeTrialResultsPreviousYear[it.runnerId]!!.place, startDate.subtractYear(1).getYearString())))
                }.sortedBy { it.timeDifference.calculateSecondsFrom() }

    }

    fun getRankedProgressionSinceTimeTrial(startDate: Date, endDate: Date, adjustForMeetDistance: Boolean): List<TimeTrialImprovementDTO> {


        val adjustedTimeTrialResults = getAllAdjustedTimeTrials(startDate, endDate)

        val runners = adjustedTimeTrialResults
                .map { it.runnerId to runnerRepository.findById(it.runnerId).get() }
                .toMap()

        val seasonBests = seasonBestService.getAllSeasonBests(startDate, endDate, adjustForMeetDistance)
                .filter { it.runner.id in runners.keys }
                .map { runners[it.runner.id]!!.id to it }
                .toMap()

        val ranks = adjustedTimeTrialResults
                .map { it to seasonBests[it.runnerId] }
                .toMap()
                .map {
                     // if seasonBestDTO is null, difference is 0, or filter them out and give no rank.

                    if (it.value == null) {
                        return@map (TimeTrialImprovementDTO(0, runners[it.key.runnerId]!!, it.key.time, "00:00", "00:00"))
                    } else {
                        return@map TimeTrialImprovementDTO(0, runners[it.key.runnerId]!!, it.key.time, it.value!!.seasonBest.first().time,
                                (it.value!!.seasonBest.first().time.calculateSecondsFrom() - it.key.time.calculateSecondsFrom()).toMinuteSecondString())
                    }


                }.sortedBy { it.improvement.calculateSecondsFrom() }

        return if (ranks.all { it.seasonBest == "00:00" }) {
            ranks
        } else {
            ranks.mapIndexed { index, it ->
                TimeTrialImprovementDTO(index + 1, it.runner, it.adjustedTimeTrial, it.seasonBest, it.improvement) }
        }


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

    fun runTTestBetweenTimeTrialDifferencesForReturningRunners(
            startDate1: Date,
            endDate1: Date,
            startDate2: Date,
            endDate2: Date,
            adjustForMeetDistance: Boolean
    ): TTestResponse {


        val timeTrialDataYear1 = getTimeTrialComparisonsBetweenYearsForSameRunners(startDate1, endDate1)
        val timeTrialDataYear2 = getTimeTrialComparisonsBetweenYearsForSameRunners(startDate2, endDate2)

        val statistcalDistributionYear1 = StatisticalComparisonDTO.from("${startDate1.getYearString()} - ${startDate1.subtractYear(1).getYearString()} time trial improvement for returning runners. Positive numbers = faster", timeTrialDataYear1.map { it.timeDifference.calculateSecondsFrom() }, "time", 4)
        val statistcalDistributionYear2 = StatisticalComparisonDTO.from("${startDate2.getYearString()} - ${startDate2.subtractYear(1).getYearString()} time trial improvement for returning runners. Positive numbers = faster", timeTrialDataYear2.map { it.timeDifference.calculateSecondsFrom() }, "time", 4)

        val tStat = TStatDTO("2 sample T test for mean difference in previous SB times to following year's Time Trial",
                TestUtils.tTest(timeTrialDataYear1.map{ it.timeDifference.calculateSecondsFrom()}.toDoubleArray(),
                        timeTrialDataYear2.map{ it.timeDifference.calculateSecondsFrom()}.toDoubleArray())
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