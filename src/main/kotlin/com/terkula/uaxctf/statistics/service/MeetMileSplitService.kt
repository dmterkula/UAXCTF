package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statisitcs.model.*
import com.terkula.uaxctf.statistics.dto.*
import com.terkula.uaxctf.statistics.dto.leaderboard.RankedAchievementDTO
import com.terkula.uaxctf.statistics.dto.personalizedsplits.PersonalizedSplitDTO
import com.terkula.uaxctf.statistics.exception.RunnerNotFoundByPartialNameException
import com.terkula.uaxctf.statistics.repository.MeetMileSplitRepository
import com.terkula.uaxctf.statistics.repository.MeetRepository
import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.statistics.request.MeetSplitsOption
import com.terkula.uaxctf.statistics.request.SortingMethodContainer
import com.terkula.uaxctf.statistics.response.RunnerMeetSplitResponse
import com.terkula.uaxctf.statistics.response.TTestResponse
import com.terkula.uaxctf.util.*
import com.terkula.uaxctf.util.TimeUtilities.Companion.getFirstDayOfGivenYear
import com.terkula.uaxctf.util.TimeUtilities.Companion.getLastDayOfGivenYear
import com.terkula.uaxctf.util.TimeUtilities.Companion.getLastDayOfYear
import org.nield.kotlinstatistics.standardDeviation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.lang.Exception
import java.sql.Date
import java.util.concurrent.Future
import kotlin.math.absoluteValue

@Component
class MeetMileSplitService(
    @field:Autowired
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
    private val personalRecordService: PersonalRecordService,
    @field:Autowired
    private val meetSplitsAsyncService: MeetMileSplitAsyncService
) {


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

    fun getMeetSplitsComparedToInputPace(meetName: String, startDate: Date, endDate: Date, targetPace: String, sortOnMile: String): List<MeetSplitsToComparisonPaceDTO> {
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
                            splitComparison.rankOnTeam = runnersToFirstMileSplitComparisons.sortedBy { it.second.percentOfComparison }.indexOfFirst { it.first == runnerToSplits.first } + 1
                            splitComparison.percentileOfTeam = (splitComparison.rankOnTeam.toDouble() / runnersToFirstMileSplitComparisons.size).round(2) * 100
                            val standardDev = runnersToFirstMileSplitComparisons.map { it.second.percentOfComparison }.standardDeviation()
                            val mean = runnersToFirstMileSplitComparisons.map { it.second.percentOfComparison }.average()
                            splitComparison.standardDeviationsFromMean = ((splitComparison.percentOfComparison - mean) / standardDev).round(2).absoluteValue
                        }
                        1 -> {
                            splitComparison.rankOnTeam = runnersToSecondMileSplitComparisons.sortedBy { it.second.percentOfComparison }.indexOfFirst { it.first == runnerToSplits.first } + 1
                            splitComparison.percentileOfTeam = (splitComparison.rankOnTeam.toDouble() / runnersToSecondMileSplitComparisons.size).round(2) * 100
                            val standardDev = runnersToSecondMileSplitComparisons.map { it.second.percentOfComparison }.standardDeviation()
                            val mean = runnersToSecondMileSplitComparisons.map { it.second.percentOfComparison }.average()
                            splitComparison.standardDeviationsFromMean = ((splitComparison.percentOfComparison - mean) / standardDev).round(2).absoluteValue
                        }
                        else -> {
                            splitComparison.rankOnTeam = runnersToThirdMileSplitComparisons.sortedBy { it.second.percentOfComparison }.indexOfFirst { it.first == runnerToSplits.first } + 1
                            splitComparison.percentileOfTeam = (splitComparison.rankOnTeam.toDouble() / runnersToThirdMileSplitComparisons.size).round(2) * 100
                            val standardDev = runnersToThirdMileSplitComparisons.map { it.second.percentOfComparison }.standardDeviation()
                            val mean = runnersToThirdMileSplitComparisons.map { it.second.percentOfComparison }.average()
                            splitComparison.standardDeviationsFromMean = ((splitComparison.percentOfComparison - mean) / standardDev).round(2).absoluteValue
                        }
                    }
                }
            }
        }

        var response =  runnerIdToSplitComparisons.map {
            runnerRepository.findById(it.first).get().name to it.second
        }.filter {
            it.second.all { splitComp -> splitComp.percentOfComparison < 2 }
        }

        response = when (sortOnMile) {
            "1" -> {
                response.sortedBy { it.second[0].percentOfComparison }
            }
            "2" -> {
                response.sortedBy { it.second[1].percentOfComparison }
            }
            "3" -> {
                response.sortedBy { it.second[2].percentOfComparison }
            }
            else -> {
                response
            }
        }

        return response.map { MeetSplitsToComparisonPaceDTO(it.first, it.second) }
    }

    fun getMeetSplitComparisonsForRunner(runnerName: String, startDate: Date, endDate: Date, targetPace: String): RunnerMeetSplitsComparisonWithAveragesDTO {

        val runner = try {
            runnerRepository.findByNameContaining(runnerName).first()
        } catch (e: Exception) {
            throw RunnerNotFoundByPartialNameException("no runner found by: $runnerName")
        }
        val meets = meetRepository.findByDateBetween(startDate, endDate)

        val runnerMeetSplitsComparisonDTOs = meets.map { it to getMeetSplitsComparedToInputPace(it.name, startDate, endDate, targetPace, "").filter { split -> split.runnerName == runner.name } }
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

    fun getMeetSplitComparisonsAveragesForAllRunners(startDate: Date, endDate: Date, targetPace: String, sortOnMile: String): List<RunnerSplitComparisonAveragesDTO> {

        val meets = meetRepository.findByDateBetween(startDate, endDate)

        val runnerMeetSplitsComparisonDTOs = meets.map { it to getMeetSplitsComparedToInputPace(it.name, startDate, endDate, targetPace, "") }
                .map {
                    RunnersMeetSplitsComparisonPaceDTO(it.first, it.second)
                }.filter {
                    it.splits.isNotEmpty()
                }.map {
                    it.splits
                }.flatten().groupBy { it.runnerName }
                .map {
                    val firstMileAverages = calculateAverageForSplit(it.value.map { it.splits[0] })
                    val secondMileAverages = calculateAverageForSplit(it.value.map { it.splits[1] })
                    val thirdMileAverages = calculateAverageForSplit(it.value.map { it.splits[2] })

                    RunnerSplitComparisonAveragesDTO(it.key, listOf(firstMileAverages, secondMileAverages, thirdMileAverages))
                }

        return when (sortOnMile) {
            "1" -> {
                runnerMeetSplitsComparisonDTOs.sortedBy { it.splitAverages[0].averageComparisonPace }
            }
            "2" -> {
                runnerMeetSplitsComparisonDTOs.sortedBy { it.splitAverages[1].averageComparisonPace }
            }
            "3" -> {
                runnerMeetSplitsComparisonDTOs.sortedBy { it.splitAverages[2].averageComparisonPace }
            }
            else -> {
                /// no sort if unrecognized sort param
                runnerMeetSplitsComparisonDTOs
            }
        }

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

        return meetSplitsAsyncService.runTwoSampleTTestForMileSplits(filterMeet, startDate1, endDate1, startDate2, endDate2, comparisonPace)
    }

    fun runTwoSampleTTestForMileSplitsForSameMeetsInSeasons(
        baseSeason: String,
        comparisonSeason: String,
        comparisonPace: String
    ): List<TTestResponse> {

        val baseYearMeets = meetRepository.findByDateBetween(getFirstDayOfGivenYear(baseSeason), getLastDayOfGivenYear(baseSeason))

        val comparisonYearMeets = meetRepository.findByDateBetween(getFirstDayOfGivenYear(comparisonSeason), getLastDayOfGivenYear(comparisonSeason))

        val commonMeets = baseYearMeets.filter { comparisonYearMeets.any {compMeet -> compMeet.name == it.name} }

        val comparisonsFutures = mutableListOf<Future<TTestResponse>>()

        commonMeets.forEach {

            val filterMeet = it.name
            val startDate1 = getFirstDayOfGivenYear(baseSeason)
            val endDate1 = getLastDayOfGivenYear(baseSeason)

            val startDate2 = getFirstDayOfGivenYear(comparisonSeason)
            val endDate2 = getLastDayOfGivenYear(comparisonSeason)

            comparisonsFutures.add(meetSplitsAsyncService.runTwoSampleTTestForMileSplitsForAllMeets(filterMeet, startDate1, endDate1, startDate2, endDate2, comparisonPace))
        }

        val comparisons = comparisonsFutures.map {
            try {
                it.get()
            } catch (e: Exception) {
                null
            }

        }

        return comparisons
                .filter { it != null }
                .map { it!! }
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


    fun getMeetSplitsForRunner(runnerId: Int): RunnerMeetSplitResponse {
        val meets = meetRepository.findAll()

        val meetMap = meets.map { it.id to it }.toMap()

        val runner = runnerRepository.findById(runnerId).get()

        val races = meetPerformanceService.getPerformancesForRunner(runnerId)

        val splitPerformance = races.map {
            it to meetMileSplitRepository.findByMeetIdAndRunnerId(it.meetId, runner.id).firstOrNull()
        }
                .filter {  it.second != null }
                .map {
                    it.first.toMeetPerformanceDTO(meetMap[it.first.meetId]!!) to it.second!!
                }
                .map {
                    RunnerMeetSplitDTO(it.first, MeetSplitsDTO(it.second.mileOne, it.second.mileTwo, it.second.mileThree))
                }

        return RunnerMeetSplitResponse(runner, splitPerformance)
    }

    fun getRankedConsistentMeetSplitsAchievement(season: String): List<RankedAchievementDTO> {
        val meets = meetRepository.findByDateBetween(TimeUtilities.getFirstDayOfGivenYear(season), TimeUtilities.getLastDayOfGivenYear(season))

        val meetMap = meets.map { it.id to it }.toMap()
        val runners = runnerRepository.findAll().associateBy { it.id }

        return meets.map { it to meetPerformanceService.getResultsForMeet(it.id) }
                .map {
                    it.second
                }.flatten()
                .groupBy { it.runnerId }
                .map {
                    runners[it.key]!! to it.value.map { result -> meetMileSplitRepository.findByMeetIdAndRunnerId(result.meetId, result.runnerId).firstOrNull() }.filterNotNull()
                }
                .map {
                    it.first to it.second.filter { it.isConsistentRace() }
                }
                .map {
                    it.first to it.second.size
                }
                .sortedByDescending { it.second }
                .mapIndexed { index, pair ->
                    RankedAchievementDTO(pair.first, index + 1, pair.second)
                }
    }

    fun getMeetSplitsForRunnersCareer(): Map<Runner, List<MeetSplitsDTO>> {
        val meets = meetRepository.findAll()

        val runners = runnerRepository.findAll()

        return runners.map { it to meetPerformanceService.getPerformancesForRunner(it.id) }
                .filter { !it.second.isEmpty()}
                .map {
                    it.first to it.second!!
                }
                .map {
                     it.first to it.second.map { race -> meetMileSplitRepository.findByMeetIdAndRunnerId(race.meetId, race.runnerId).firstOrNull() }
                    }
                .map {
                    it.first to it.second.filterNotNull().map { split -> MeetSplitsDTO(split.mileOne, split.mileTwo, split.mileThree) }
                }.toMap()
    }

    fun getPersonalizedSplitsToHitCertainTime(
            runnerId: Int,
            time: String,
            lastNRaces: Int?,
            meetName: String?
    ): PersonalizedSplitDTO {

       val splits = getMeetSplitsForRunner(runnerId)

        val mileSplits = if (lastNRaces != null) {
            splits.mileSplits.sortedByDescending { it.meetPerformanceDTO.meetDate }.take(lastNRaces)
        } else {
            splits.mileSplits
        }

        val mile1RatioOfTimeAvg = mileSplits.map {
            it.meetSplitsDTO!!.mileOne.calculateSecondsFrom() / it.meetPerformanceDTO.time.calculateSecondsFrom()
        }.average()

        val mile2RatioOfTimeAvg = mileSplits.map {
            it.meetSplitsDTO!!.mileTwo.calculateSecondsFrom() / it.meetPerformanceDTO.time.calculateSecondsFrom()
        }.average()

        val mile3RatioOfTimeAvg = mileSplits.map {
            it.meetSplitsDTO!!.mileThree.calculateSecondsFrom() / it.meetPerformanceDTO.time.calculateSecondsFrom()
        }.average()

        var targetMile1 = (mile1RatioOfTimeAvg * time.calculateSecondsFrom()).toMinuteSecondString()
        var targetMile2 = (mile2RatioOfTimeAvg * time.calculateSecondsFrom()).toMinuteSecondString()
        var targetMile3 = (mile3RatioOfTimeAvg * time.calculateSecondsFrom()).toMinuteSecondString()


        val mile1ToMile2TeamAvg: Double?
        val mile2ToMile3TeamAvg: Double?
        val indivMile1ToMile2Difference: Double?
        val indivMile2ToMile3Difference: Double?

        if (meetName != null) {
            val teamAverages = getStatisticsForMeet(meetName, getFirstDayOfGivenYear("2017"), getLastDayOfYear())

            if (teamAverages.all { it.value != 0.0 }) {
                mile1ToMile2TeamAvg = teamAverages[0].value
                mile2ToMile3TeamAvg = teamAverages[1].value

                indivMile1ToMile2Difference = (mile2RatioOfTimeAvg * time.calculateSecondsFrom()) - (mile1RatioOfTimeAvg * time.calculateSecondsFrom())
                indivMile2ToMile3Difference = (mile3RatioOfTimeAvg * time.calculateSecondsFrom()) - (mile2RatioOfTimeAvg * time.calculateSecondsFrom())

                // if both negative or both positive, subtract and take divide by 2
                // if not the same sign, add and divide by 2


                var combinedMile1ToMile2Difference = if (indivMile1ToMile2Difference * mile1ToMile2TeamAvg >= 0) {
                    // same sign, take difference
                    (mile1ToMile2TeamAvg - indivMile1ToMile2Difference) / 2
                } else {
                    // opposite sign, take difference by adding
                    (mile1ToMile2TeamAvg + indivMile1ToMile2Difference) / 2
                }

                var combinedMile2ToMile3Difference = if (indivMile2ToMile3Difference * mile2ToMile3TeamAvg >= 0) {
                    // same sign, take difference
                    (mile2ToMile3TeamAvg - indivMile2ToMile3Difference) / 2
                } else {
                    // opposite sign, take difference by adding
                    (mile2ToMile3TeamAvg + indivMile2ToMile3Difference) / 2
                }

                var target1MileSeconds = (mile1RatioOfTimeAvg * time.calculateSecondsFrom())
                var target2MileSeconds = (mile2RatioOfTimeAvg * time.calculateSecondsFrom())
                var target3MileSeconds = (mile3RatioOfTimeAvg * time.calculateSecondsFrom())

                if (combinedMile1ToMile2Difference >= 0) {
                    target1MileSeconds -= combinedMile1ToMile2Difference
                    target2MileSeconds += combinedMile1ToMile2Difference
                } else {
                    target1MileSeconds += combinedMile1ToMile2Difference
                    target2MileSeconds -= combinedMile1ToMile2Difference
                }

                if (combinedMile2ToMile3Difference >= 0) {
                    target2MileSeconds -= combinedMile2ToMile3Difference
                    target3MileSeconds += combinedMile2ToMile3Difference
                } else {
                    target2MileSeconds += combinedMile2ToMile3Difference
                    target3MileSeconds -= combinedMile2ToMile3Difference
                }

                targetMile1 = target1MileSeconds.toMinuteSecondString()
                targetMile2 = target2MileSeconds.toMinuteSecondString()
                targetMile3 = target3MileSeconds.toMinuteSecondString()
            }

        }

        val avgSplit = (time.calculateSecondsFrom() / 3.10686).toMinuteSecondString()

        return PersonalizedSplitDTO(avgSplit, targetMile1, targetMile2, targetMile3)

    }

}