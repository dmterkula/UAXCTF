package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statisitcs.model.*
import com.terkula.uaxctf.statistics.dto.MeetSplitStatisticDTO
import com.terkula.uaxctf.statistics.dto.MeetSplitsDTO
import com.terkula.uaxctf.statistics.dto.RunnerAvgMileSplitDifferenceDTO
import com.terkula.uaxctf.statistics.dto.RunnerMeetSplitDTO
import com.terkula.uaxctf.statistics.exception.RunnerNotFoundByPartialNameException
import com.terkula.uaxctf.statistics.repository.MeetMileSplitRepository
import com.terkula.uaxctf.statistics.repository.MeetRepository
import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.statistics.request.MeetSplitsOption
import com.terkula.uaxctf.statistics.request.SortingMethodContainer
import com.terkula.uaxctf.statistics.response.RunnerMeetSplitResponse
import com.terkula.uaxctf.util.calculateSecondsFrom
import com.terkula.uaxctf.util.toMinuteSecondString
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.lang.Exception
import java.sql.Date

@Component
class MeetMileSplitService(@field:Autowired
                           private val meetMileSplitRepository: MeetMileSplitRepository,
                           @field:Autowired
                           private val meetRepository: MeetRepository,
                           @field:Autowired
                           private val runnerRepository: RunnerRepository,
                           @field:Autowired
                           private val meetPerformanceService: MeetPerformanceService) {


    fun getAllMeetMileSplitsForRunner(name: String, startDate: Date, endDate: Date): RunnerMeetSplitResponse {
        val meets = meetRepository.findByDateBetween(startDate, endDate)

        val runner = try {
            runnerRepository.findByNameContaining(name).first()
        } catch (e: Exception) {
            throw RunnerNotFoundByPartialNameException("np runner found by: $name")
        }

        val performances = meetPerformanceService.getMeetPerformancesForRunnerWithNameContaining(name,
                startDate, endDate, SortingMethodContainer.TIME, 20).map { it.performance }
                .flatten()
                .filter {
                    it.meetName in meets.map { meet -> meet.name }
                }.map {
                    meetRepository.findByNameContainsAndDateBetween(it.meetName, startDate, endDate).first().id to it
                }.toMap()

        return RunnerMeetSplitResponse(runner, meets.map { it.id to meetMileSplitRepository.findByMeetIdAndRunnerId(it.id, runner.id).firstOrNull() }
                .filter { it.second != null }
                .map { it.first to it.second!!}
                .toMap()
                .map {
                    RunnerMeetSplitDTO(performances[it.key]!!, MeetSplitsDTO(it.value.mileOne, it.value.mileTwo, it.value.mileThree))
                })

    }


    fun getMeetSplitInfo(filterMeet: String, splitType: MeetSplitsOption, startDate: Date, endDate: Date, sort: String,
                         limit: Int): List<RunnerAvgMileSplitDifferenceDTO> {
        // all meets in date range

        val meets = if( filterMeet.isEmpty()) {
            meetRepository.findByDateBetween(startDate, endDate)
        } else {
            meetRepository.findByNameContainsAndDateBetween(filterMeet, startDate, endDate)
        }

        // all splits at meets in date range
        val splits = meets.
                map { meetMileSplitRepository.findByMeetId(it.id) }
                .filter { it.isNotEmpty() }
                .flatten().groupBy { it.runnerId }

        // all runners with splits
        val runners = splits.map { runnerRepository.findById(it.key).get() }.map { it.id to it }.toMap()

        val runnerAverages = when (splitType.value) {
            MeetSplitsOption.FirstToSecondMile.value -> {
                splits.map {
                    Triple<Runner, List<Double>, Int>(runners[it.key]!!, it.value.map { meetSplits -> meetSplits.calculateFirstToSecondMileDifference() }, it.value.size)
                }

            }
            MeetSplitsOption.SecondToThirdMile.value -> {
                splits.map {
                    Triple<Runner, List<Double>, Int>(runners[it.key]!!, it.value.map { meetSplits -> meetSplits.calculateSecondToThirdMileDifference() }, it.value.size)
                }

            }
            MeetSplitsOption.Spread.value -> {
                splits.map {
                    Triple<Runner, List<Double>, Int> (runners[it.key]!!, it.value.map { meetSplits -> meetSplits.calculateSpread() }, it.value.size)
                }
            }
            else -> {
                splits.map {
                    Triple<Runner, List<Double>, Int> (runners[it.key]!!, it.value.map { meetSplits -> meetSplits.calculateTotalDifferenceBetween() }, it.value.size)
                }

            }
        }

        val splitDTOs = runnerAverages.map {
            RunnerAvgMileSplitDifferenceDTO(it.first, splitType.value, it.second.average().toMinuteSecondString(), it.third )
        }.toMutableList()

        return when (sort) {
            "lowest" -> splitDTOs.sortedBy { it.avgDifference.calculateSecondsFrom() }
            else -> splitDTOs.sortedByDescending{ it.avgDifference.calculateSecondsFrom() }
        }.take(limit)
    }

    fun findMeetWithSignificantSplitStat(startDate: Date, endDate: Date, splitType: MeetSplitsOption, sort: String): List<MeetSplitStatisticDTO> {

        val meets = meetRepository.findByDateBetween(startDate, endDate)

        val meetStatisticDTOs = meets.map { it to meetMileSplitRepository.findByMeetId(it.id) }.toMap()
                .filter { it.value.isNotEmpty() }
                .map {
                    it.key to calculateDesiredMeetStatistic(splitType, it.value)
                }.map {
                    MeetSplitStatisticDTO(it.first, splitType.value, it.second)
                }.toMutableList()

        return when (sort) {
            "lowest" -> meetStatisticDTOs.sortedBy { it.value }
            else -> meetStatisticDTOs.sortedByDescending { it.value }
        }
    }

    fun calculateDesiredMeetStatistic(splitType: MeetSplitsOption, meetSplits: List<MeetMileSplit>): Double {

        return when (splitType.value) {
            MeetSplitsOption.FirstToSecondMile.value -> meetSplits.calculateAverageFirstToSecondMileDifference()
            MeetSplitsOption.SecondToThirdMile.value -> meetSplits.calculateAverageSecondToThirdMileDifference()
            MeetSplitsOption.Spread.value -> meetSplits.calculateAverageSpread()
            else -> meetSplits.calculateTotalAverageDifference()
        }

    }

    fun getStatisticsForMeet(filterMeet: String, startDate: Date, endDate: Date): List<NamedStatistic> {
        // all meets in date range
        val meets = if( filterMeet.isEmpty()) {
            meetRepository.findByDateBetween(startDate, endDate)
        } else {
            meetRepository.findByNameContainsAndDateBetween(filterMeet, startDate, endDate)
        }

        // all splits at meets in date range
        val splits = meets.
                map { meetMileSplitRepository.findByMeetId(it.id) }
                .filter { it.isNotEmpty() }
                .flatten()

        return listOf(NamedStatistic(MeetSplitsOption.FirstToSecondMile.value, calculateDesiredMeetStatistic(MeetSplitsOption.FirstToSecondMile, splits)),
                NamedStatistic(MeetSplitsOption.SecondToThirdMile.value, calculateDesiredMeetStatistic(MeetSplitsOption.SecondToThirdMile, splits)),
                NamedStatistic(MeetSplitsOption.Combined.value, calculateDesiredMeetStatistic(MeetSplitsOption.Combined, splits)),
                NamedStatistic(MeetSplitsOption.Spread.value, calculateDesiredMeetStatistic(MeetSplitsOption.Spread, splits)))

    }

}