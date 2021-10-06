package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statisitcs.model.*
import com.terkula.uaxctf.statistics.dto.*
import com.terkula.uaxctf.statistics.exception.RunnerNotFoundByPartialNameException
import com.terkula.uaxctf.statistics.repository.MeetMileSplitRepository
import com.terkula.uaxctf.statistics.repository.MeetRepository
import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.statistics.request.MeetSplitsOption
import com.terkula.uaxctf.statistics.request.SortingMethodContainer
import com.terkula.uaxctf.statistics.response.RunnerMeetSplitResponse
import com.terkula.uaxctf.statistics.response.TTestResponse
import com.terkula.uaxctf.util.*
import org.apache.commons.math3.stat.inference.TestUtils.tTest
import org.nield.kotlinstatistics.standardDeviation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.lang.Exception
import java.sql.Date
import kotlin.math.absoluteValue

@Component
class MeetMileSplitService(@field:Autowired
                           private val meetMileSplitRepository: MeetMileSplitRepository,
                           @field:Autowired
                           private val meetRepository: MeetRepository,
                           @field:Autowired
                           private val runnerRepository: RunnerRepository,
                           @field:Autowired
                           private val meetPerformanceService: MeetPerformanceService,
                           @field:Autowired
                           private val seasonBestService: SeasonBestService,
                           @field:Autowired
                           private val personalRecordService: PersonalRecordService) {


    fun getAllMeetMileSplitsForRunner(name: String, startDate: Date, endDate: Date): RunnerMeetSplitResponse {
        val meets = meetRepository.findByDateBetween(startDate, endDate)

        // val meetMap = meets.map { it.id to it }.toMap()

        val runner = try {
            runnerRepository.findByNameContaining(name).first()
        } catch (e: Exception) {
            throw RunnerNotFoundByPartialNameException("no runner found by: $name")
        }

        val performances: Map<Int, MeetPerformanceDTO> = meetPerformanceService.getMeetPerformancesForRunnerWithNameContaining(name,
                startDate, endDate, SortingMethodContainer.TIME, 20, false).map { it.performance }
                .flatten()
                .filter {
                    it.meetName in meets.map { meet -> meet.name }
                }.map {
                    meetRepository.findByNameAndDateBetween(it.meetName, startDate, endDate).first().id to it
                }.toMap()

        return RunnerMeetSplitResponse(runner, meets.map { it.id to meetMileSplitRepository.findByMeetIdAndRunnerId(it.id, runner.id).firstOrNull() }
                .filter { it.second != null }
                .map { it.first to it.second!!}
                .toMap()
                .map {
                    //val prAsOfThisMeetDate = personalRecordService.getPRsByNameBeforeTargetDate(runner.name, false, meetMap[it.key]!!.date)
                    RunnerMeetSplitDTO(performances[it.key]!!, MeetSplitsDTO(it.value.mileOne, it.value.mileTwo, it.value.mileThree))
                })
    }

    fun getMeetSplitsComparedToInputPace(meetName: String, startDate: Date, endDate: Date, targetPace: String): List<MeetSplitsToComparisonPaceDTO> {
        val meets = meetRepository.findByNameAndDateBetween(meetName, startDate, endDate)

        val meetMap = meets.map { it.id to it }.toMap()

        // all splits at meets in date range
        val splits = meets.map { meetMileSplitRepository.findByMeetId(it.id) }
                .filter { it.isNotEmpty() }
                .take(1)
                .flatten().groupBy { it.runnerId }

        val numberOfRunnersInMeet = splits.values.size

        // all runners with splits
        val runners = splits.map { runnerRepository.findById(it.key).get() }.map { it.id to it }.toMap()


        val runnerIdToSplitComparisons = splits.map {
            it.key to it.value.map { split->

                val prBeforeThisMeet = personalRecordService.getPRsByNameBeforeTargetDate(runners[it.key]!!.name, false, meetMap[split.meetId]!!.date.subtractDay())

                val prPaceNullable = prBeforeThisMeet.firstOrNull()?.pr?.firstOrNull()?.time?.getPacePerMile()
                val prPace = if (prPaceNullable == null) {
                    // use a value that will create a very higher number that we can filter out later to avoid this screwing up the metrics
                    1.0
                } else {
                    prPaceNullable!!
                }


                val percentOfPrPaceMile1: Double = (split.mileOne.calculateSecondsFrom() / prPace).round(2)
                val percentOfPrPaceMile2: Double = (split.mileTwo.calculateSecondsFrom() / prPace).round(2)
                val percentOfPrPaceMile3: Double = (split.mileThree.calculateSecondsFrom() / prPace).round(2)

                listOf(MeetSplitToComparisonPace(1, split.mileOne, percentOfPrPaceMile1, 0, 0.0, 0.0),
                        MeetSplitToComparisonPace(2, split.mileTwo, percentOfPrPaceMile2, 0, 0.0, 0.0),
                        MeetSplitToComparisonPace(3, split.mileThree, percentOfPrPaceMile3, 0, 0.0, 0.0),
                )
            }.flatten()
        }

        val runnersToFirstMileSplitComparisons = runnerIdToSplitComparisons.map {
            it.first to it.second[0]
        }.filter{it.second.percentOfComparison < 2 }.sortedBy { it.second.percentOfComparison }

        val runnersToSecondMileSplitComparisons = runnerIdToSplitComparisons.map {
            it.first to it.second[1]
        }.filter{it.second.percentOfComparison < 2 }.sortedBy { it.second.percentOfComparison }

        val runnersToThirdMileSplitComparisons = runnerIdToSplitComparisons.map {
            it.first to it.second[2]
        }.filter{it.second.percentOfComparison < 2 }.sortedByDescending { it.second.percentOfComparison }

        runnerIdToSplitComparisons.forEach { runnerToSplits->
            runnerToSplits.second.forEachIndexed{index, splitComparison ->
                run {
                    when (index) {
                        0 -> {
                            splitComparison.rankOnTeam = runnersToFirstMileSplitComparisons.indexOfFirst { it.first == runnerToSplits.first } + 1
                            splitComparison.percentileOfTeam = (splitComparison.rankOnTeam.toDouble() / runnersToFirstMileSplitComparisons.size).round(2) * 100
                            val standardDev = runnersToFirstMileSplitComparisons.map { it.second.percentOfComparison }.standardDeviation()
                            val mean = runnersToFirstMileSplitComparisons.map { it.second.percentOfComparison }.average()
                            splitComparison.standardDeviationsFromMean = ((splitComparison.percentOfComparison - mean) / standardDev).round(2).absoluteValue
                        }
                        1 -> {
                            splitComparison.rankOnTeam = runnersToSecondMileSplitComparisons.indexOfFirst { it.first == runnerToSplits.first } + 1
                            splitComparison.percentileOfTeam = (splitComparison.rankOnTeam.toDouble() / runnersToSecondMileSplitComparisons.size).round(2) * 100
                            val standardDev = runnersToSecondMileSplitComparisons.map { it.second.percentOfComparison }.standardDeviation()
                            val mean = runnersToSecondMileSplitComparisons.map { it.second.percentOfComparison }.average()
                            splitComparison.standardDeviationsFromMean = ((splitComparison.percentOfComparison - mean) / standardDev).round(2).absoluteValue
                        }
                        else -> {
                            splitComparison.rankOnTeam = runnersToThirdMileSplitComparisons.indexOfFirst { it.first == runnerToSplits.first } + 1
                            splitComparison.percentileOfTeam = (splitComparison.rankOnTeam.toDouble() / runnersToThirdMileSplitComparisons.size).round(2) * 100
                            val standardDev = runnersToThirdMileSplitComparisons.map { it.second.percentOfComparison }.standardDeviation()
                            val mean = runnersToThirdMileSplitComparisons.map { it.second.percentOfComparison }.average()
                            splitComparison.standardDeviationsFromMean = ((splitComparison.percentOfComparison - mean) / standardDev).round(2).absoluteValue
                        }
                    }
                }
            }
        }

        return runnerIdToSplitComparisons.map {
            runnerRepository.findById(it.first).get().name to it.second
        }.filter {
            it.second.all { splitComp -> splitComp.percentOfComparison < 2 }
        }.map {
            MeetSplitsToComparisonPaceDTO(it.first, it.second)
        }

    }

    fun getMeetSplitComparisonsForRunner(runnerName: String, startDate: Date, endDate: Date, targetPace: String): RunnerMeetSplitsComparisonWithAveragesDTO {

        val runner = try {
            runnerRepository.findByNameContaining(runnerName).first()
        } catch (e: Exception) {
            throw RunnerNotFoundByPartialNameException("no runner found by: $runnerName")
        }
        val meets = meetRepository.findByDateBetween(startDate, endDate)

        val runnerMeetSplitsComparisonDTOs = meets.map { it to getMeetSplitsComparedToInputPace(it.name, startDate, endDate, targetPace).filter { split -> split.runnerName == runner.name } }
                .map {
           RunnersMeetSplitsComparisonPaceDTO(it.first, it.second)
        }.filter {
            it.splits.isNotEmpty()
               }

        val firstMileAverages = calculateAverageForSplit(runnerMeetSplitsComparisonDTOs.map { it.splits[0].splits[0] })
        val secondMileAverages = calculateAverageForSplit(runnerMeetSplitsComparisonDTOs.map { it.splits[0].splits[1] })
        val thirdMileAverages = calculateAverageForSplit(runnerMeetSplitsComparisonDTOs.map { it.splits[0].splits[2] })

        return RunnerMeetSplitsComparisonWithAveragesDTO(runnerMeetSplitsComparisonDTOs, listOf(firstMileAverages, secondMileAverages, thirdMileAverages))

    }

    fun calculateAverageForSplit(splitsForParticularMile: List<MeetSplitToComparisonPace>): SplitComparisonAverageDTO {
        return SplitComparisonAverageDTO(splitsForParticularMile[0].mileNumber,
                splitsForParticularMile.map { it.rankOnTeam.toDouble() }.average().round(2),
                splitsForParticularMile.map { it.percentileOfTeam }.average().round(2),
                splitsForParticularMile.map { it.percentOfComparison }.average().round(2),
        )
    }

    fun getMeetSplitInfo(filterMeet: String, splitType: MeetSplitsOption, startDate: Date, endDate: Date, sort: String,
                         limit: Int): List<RunnerAvgMileSplitDifferenceDTO> {
        // all meets in date range

        val meets = if( filterMeet.isEmpty()) {
            meetRepository.findByDateBetween(startDate, endDate)
        } else {
            meetRepository.findByNameAndDateBetween(filterMeet, startDate, endDate)
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
            meetRepository.findByNameAndDateBetween(filterMeet, startDate, endDate)
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

    fun compareMileSplitTimesToComparisonPaceAtMeet(filterMeet: String, startDate: Date, endDate: Date, comparisonPace: String):
    List<StatisticalComparisonDTO>{
        // all meets in date range
        val meets = meetRepository.findByNameAndDateBetween(filterMeet, startDate, endDate)

        val ratiosOfMileSplitsToInputMetric = getMeetSplitsToComparision(filterMeet, comparisonPace, startDate, endDate)

        val labelPrefix = "${meets.first().name} ${startDate.getYearString()}:"

        val label: String = if (comparisonPace.equals("SB", true)) {
            "percent of previous season best pace "
        } else {
            "percent of PR pace prior to this meet"
        }

        val mile1Distribution = StatisticalComparisonDTO.from("$labelPrefix mile 1 $label", ratiosOfMileSplitsToInputMetric.map {it.first }, "decimal", 4)
        val mile2Distribution = StatisticalComparisonDTO.from("$labelPrefix mile 2 $label", ratiosOfMileSplitsToInputMetric.map{ it.second }, "decimal", 4)
        val mile3Distribution = StatisticalComparisonDTO.from("$labelPrefix mile 3 $label", ratiosOfMileSplitsToInputMetric.map{ it.third }, "decimal", 4)

        return listOf(mile1Distribution, mile2Distribution, mile3Distribution)
    }

    fun buildRankedMileSplitComparisonsAcrossMeets(startDate: Date, endDate: Date, comparisonPace: String):
            List<List<MeetSplitStatisticalComparisonDTO>>{
        // all meets in date range
        val meets = meetRepository.findByDateBetween(startDate, endDate)

        val meetMileSplitStats = meets.map {
            it.name to
            compareMileSplitTimesToComparisonPaceAtMeet(it.name, startDate, endDate, comparisonPace)
        }

        val mileStatComparisons = listOf(1, 2, 3).map {
            createRankedSplitByMile(meetMileSplitStats, it)
        }

        return mileStatComparisons

    }

    private fun createRankedSplitByMile(meetMileSplitStats: List<Pair<String, List<StatisticalComparisonDTO>>>, mileNumber: Int): List<MeetSplitStatisticalComparisonDTO> {
       val rankedSplitStats = meetMileSplitStats.map {
            it.first to it.second[mileNumber - 1]
        }.sortedBy { it.second.meanDifference.toDouble() }
                .mapIndexed { index, pair -> MeetSplitStatisticalComparisonDTO(pair.first, index + 1, pair.second)}
        return rankedSplitStats
    }

    fun runTwoSampleTTestForMileSplits(
            filterMeet: String,
            startDate1: Date,
            endDate1: Date,
            startDate2: Date,
            endDate2: Date,
            comparisonPace: String
    ): TTestResponse {

        val dataYear1 = getMeetSplitsToComparision(filterMeet, comparisonPace, startDate1, endDate1)
        val dataYear2 = getMeetSplitsToComparision(filterMeet, comparisonPace, startDate2, endDate2)

        var labelPrefixYear1 = "$filterMeet ${startDate1.getYearString()}:"
        var labelPrefixYear2 = "$filterMeet ${startDate2.getYearString()}:"

        var label: String = if (comparisonPace.equals("SB", true)) {
            "percent of previous season best pace "
        } else {
            "percent of PR pace"
        }

        val mile1DistributionYear1 = StatisticalComparisonDTO.from("$labelPrefixYear1 mile 1 $label", dataYear1.map {it.first }, "decimal", 4)
        val mile2DistributionYear1 = StatisticalComparisonDTO.from("$labelPrefixYear1 mile 2 $label", dataYear1.map{ it.second }, "decimal", 4)
        val mile3DistributionYear1 = StatisticalComparisonDTO.from("$labelPrefixYear1 mile 3 $label", dataYear1.map{ it.third }, "decimal", 4)

        val mile1DistributionYear2 = StatisticalComparisonDTO.from("$labelPrefixYear2 mile 1 $label", dataYear2.map {it.first }, "decimal", 4)
        val mile2DistributionYear2 = StatisticalComparisonDTO.from("$labelPrefixYear2 mile 2 $label", dataYear2.map{ it.second }, "decimal", 4)
        val mile3DistributionYear2 = StatisticalComparisonDTO.from("$labelPrefixYear2 mile 3 $label", dataYear2.map{ it.third }, "decimal", 4)

        val tStatDTOMile1 = TStatDTO("t test comparing mile1 splits", tTest(dataYear1.map { it.first }.toDoubleArray(), dataYear2.map { it.first }.toDoubleArray()).round(4))
        val tStatDTOMile2 = TStatDTO("t test comparing mile2 splits", tTest(dataYear1.map{ it.second }.toDoubleArray(), dataYear2.map { it.second }.toDoubleArray()).round(4))
        val tStatDTOMile3 = TStatDTO("t test comparing mile3 splits", tTest(dataYear1.map{ it.third }.toDoubleArray(), dataYear2.map { it.third }.toDoubleArray()).round(4))


        return TTestResponse(
                listOf(mile1DistributionYear1, mile2DistributionYear1, mile3DistributionYear1),
                listOf(mile1DistributionYear2, mile2DistributionYear2, mile3DistributionYear2),
                listOf(tStatDTOMile1, tStatDTOMile2, tStatDTOMile3)
        )
    }


    fun getMeetSplitsToComparision(filterMeet: String,
                                   comparisonPace: String,
                                   startDate: Date,
                                   endDate: Date): List<Triple<Double, Double, Double>> {
        val meets = meetRepository.findByNameAndDateBetween(filterMeet, startDate, endDate)

        // all splits at meets in date range
        val splits = meets.
                map { meetMileSplitRepository.findByMeetId(it.id) }
                .filter { it.isNotEmpty() }
                .flatten()


        return if (comparisonPace.equals("SB", true)) {
            splits.map {
                runnerRepository.findById(it.runnerId).get() to it
            }.map {
                seasonBestService.getSeasonBestsByName(it.first.name, listOf(
                        TimeUtilities.getFirstDayOfGivenYear(startDate.subtractYear(1).getYearString()) to
                                TimeUtilities.getLastDayOfGivenYear(endDate.subtractYear(1).getYearString())),
                        false).firstOrNull() to it.second
            }
                    .filter { it.first != null }
                    .map { it.first!!.seasonBest to it.second}
                    .filter { it.first.isNotEmpty() }
                    .map { it.first.first().time.getPacePerMile() to it.second}
                    .map {
                        Triple(it.second.mileOne.calculateSecondsFrom() / it.first,
                                it.second.mileTwo.calculateSecondsFrom() / it.first,
                                it.second.mileThree.calculateSecondsFrom() / it.first)
                    }

        } else if (comparisonPace.equals("PR", true)) {
            splits.map {
                runnerRepository.findById(it.runnerId).get() to it
            }.map {
                personalRecordService.getPRsByNameBeforeTargetDate(it.first.name, false, meets[0].date.subtractDay()).firstOrNull() to it.second
            }
                    .filter { it.first != null }
                    .map { it.first!!.pr to it.second}
                    .filter { it.first.isNotEmpty() }
                    .map { it.first.first().time.getPacePerMile() to it.second}
                    .map {
                        Triple(it.second.mileOne.calculateSecondsFrom() / it.first,
                                it.second.mileTwo.calculateSecondsFrom() / it.first,
                                it.second.mileThree.calculateSecondsFrom() / it.first)
                    }

        } else {
            listOf(Triple(0.0, 0.0, 0.0))
        }
    }

}