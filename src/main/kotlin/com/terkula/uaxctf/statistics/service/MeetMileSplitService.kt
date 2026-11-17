package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statisitcs.model.*
import com.terkula.uaxctf.statistics.dto.*
import com.terkula.uaxctf.statistics.dto.leaderboard.RankedAchievementDTO
import com.terkula.uaxctf.statistics.dto.personalizedsplits.PersonalizedSplitDTO
import com.terkula.uaxctf.statistics.exception.RunnerNotFoundByPartialNameException
import com.terkula.uaxctf.statistics.repository.MeetDivisionRepository
import com.terkula.uaxctf.statistics.repository.MeetMileSplitRepository
import com.terkula.uaxctf.statistics.repository.MeetRepository
import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.statistics.repository.TimeTrialRepository
import com.terkula.uaxctf.statistics.repository.VenueRepository
import com.terkula.uaxctf.statistics.request.MeetSplitsOption
import com.terkula.uaxctf.statistics.request.SortingMethodContainer
import com.terkula.uaxctf.statistics.response.RunnerMeetSplitResponse
import com.terkula.uaxctf.statistics.response.TTestResponse
import com.terkula.uaxctf.training.repository.BaseTrainingPercentagesRepository
import com.terkula.uaxctf.training.service.TrainingBasePerformanceService
import com.terkula.uaxctf.util.*
import com.terkula.uaxctf.weather.EventWeatherResolver
import com.terkula.uaxctf.weather.HeatPaceAdjustmentService
import com.terkula.uaxctf.weather.ResolvedWeather
import com.terkula.uaxctf.util.TimeUtilities.Companion.getFirstDayOfGivenYear
import com.terkula.uaxctf.util.TimeUtilities.Companion.getLastDayOfGivenYear
import com.terkula.uaxctf.util.TimeUtilities.Companion.getLastDayOfYear
import org.apache.commons.math3.stat.inference.TestUtils
import org.nield.kotlinstatistics.standardDeviation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.lang.Exception
import java.sql.Date
import java.util.concurrent.Future
import kotlin.math.absoluteValue

private enum class PaceSource { SB, PR, TIME_TRIAL, NONE }

private class RunnerPaceComparison(
    val paceSource: PaceSource,
    val mileOnePercent: Double?,
    val mileTwoPercent: Double?,
    val mileThreePercent: Double?
)

private class MeetPaceComparisonKeyResult(
    val season: String,
    val meetName: String,
    val meet: Meet?,
    val comparisons: List<RunnerPaceComparison>
)

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
    private val meetSplitsAsyncService: MeetMileSplitAsyncService,
    val trainingBasePerformanceService: TrainingBasePerformanceService,
    @field:Autowired
    private val timeTrialRepository: TimeTrialRepository,
    @field:Autowired
    private val venueRepository: VenueRepository,
    @field:Autowired
    private val meetDivisionRepository: MeetDivisionRepository,
    @field:Autowired
    private val eventWeatherResolver: EventWeatherResolver,
    private val venueCourseProfileRepository: com.terkula.uaxctf.statistics.repository.VenueCourseProfileRepository,
    private val courseGradeAdjustmentService: com.terkula.uaxctf.weather.CourseGradeAdjustmentService,
    @field:Autowired
    private val heatPaceAdjustmentService: HeatPaceAdjustmentService
) {

    /**
     * Fallback pace-per-mile for runners with no PR on record before this meet (e.g. freshmen debuting).
     * Uses that runner's time trial for the meet's season, scaled from 3 miles to 5k-equivalent pace,
     * the same conversion TimeTrialService uses elsewhere.
     */
    private fun getTimeTrialFallbackPacePerMile(runnerId: Int, meetDate: Date): Double? {
        val timeTrial = timeTrialRepository.findByRunnerIdAndSeason(runnerId, meetDate.getYearString()).firstOrNull()
                ?: return null
        val adjusted5kSeconds = timeTrial.time.calculateSecondsFrom().scaleTimeTo5k(4827.0)
        return adjusted5kSeconds / 3.10686
    }


    fun getAllMeetMileSplitsForRunner(name: String, startDate: Date, endDate: Date): RunnerMeetSplitResponse {
        val meets = meetRepository.findByDateBetween(startDate, endDate)

         val meetMap = meets.map { it.id to it }.toMap()

        val runner = try {
            runnerRepository.findByNameContaining(name).first()
        } catch (e: Exception) {
            throw RunnerNotFoundByPartialNameException("no runner found by: $name")
        }

        val performances: Map<Int, MeetPerformanceDTO> = meetPerformanceService.getMeetPerformancesForRunnerWithNameContaining(name,
                startDate, endDate, SortingMethodContainer.TIME, 50, false).map { it.performance }
                .flatten()
                .filter {
                    it.meetName in meets.map { meet -> meet.name }
                }.map {
                    meets.firstOrNull { m-> m.name == it.meetName && m.date == it.meetDate }?.id to it
                }.filter{
                            it.first != null
                        }
                .map { it.first!! to it.second }
                .toMap()

        return RunnerMeetSplitResponse(runner, performances.map { it.key to meetMileSplitRepository.findByMeetIdAndRunnerId(it.key, runner.id).firstOrNull() }
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

                var comparisonPace: Double? = null

                if (targetPace == "baseTraining") {

                    val baseTraining = trainingBasePerformanceService.getRunnersTrainingBasePerformance(it.key, "xc", meets.first().date.getYearString())

                    if (baseTraining.trainingBasePerformance != null) {
                        comparisonPace = baseTraining.trainingBasePerformance.seconds.toDouble().toMinuteSecondString().getPacePerMile()
                    }


                } else if (targetPace == "sb") {

                    val sb = seasonBestService.getSeasonBestBeforeDate(it.key, meets.first().date)
                    if (sb != null) {
                        comparisonPace = sb.time.getPacePerMile()
                    }

                } else {
                    val prBeforeThisMeet = personalRecordService.getPRsByNameBeforeTargetDate(runners[it.key]!!.name, false, meetMap[split.meetId]!!.date.subtractDay())

                    val prPaceNullable = prBeforeThisMeet.firstOrNull()?.pr?.firstOrNull()?.time?.getPacePerMile()
                    comparisonPace = prPaceNullable ?: getTimeTrialFallbackPacePerMile(it.key, meetMap[split.meetId]!!.date)
                }

                if (comparisonPace == null) {
                    // use a value that will create a very higher number that we can filter out later to avoid this screwing up the metrics
                    comparisonPace = 1.0
                }


                val percentOfPaceMile1: Double = (split.mileOne.calculateSecondsFrom() / comparisonPace).round(2)
                val percentOfPaceMile2: Double = (split.mileTwo.calculateSecondsFrom() / comparisonPace).round(2)
                val percentOfPaceMile3: Double = (split.mileThree.calculateSecondsFrom() / comparisonPace).round(2)

                listOf(MeetSplitToComparisonPace(1, split.mileOne, percentOfPaceMile1, 0, 0.0, 0.0),
                        MeetSplitToComparisonPace(2, split.mileTwo, percentOfPaceMile2, 0, 0.0, 0.0),
                        MeetSplitToComparisonPace(3, split.mileThree, percentOfPaceMile3, 0, 0.0, 0.0),
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

    fun getStatisticsForMeetForRunners(filterMeet: String, startDate: Date, endDate: Date): List<Pair<Runner, List<NamedStatistic>>> {
        // all meets in date range
        val meets = if( filterMeet.isEmpty()) {
            meetRepository.findByDateBetween(startDate, endDate)
        } else {
            meetRepository.findByNameAndDateBetween(filterMeet, startDate, endDate)
        }

        val runners = runnerRepository.findAll().associateBy { it.id }

        // all splits at meets in date range
        val splits = meets.
        map { meetMileSplitRepository.findByMeetId(it.id) }
                .filter { it.isNotEmpty() }
                .flatten()
                .map {
                    runners[it.runnerId]!! to it
                }
                .map {
                    it.first to listOf(NamedStatistic(MeetSplitsOption.FirstToSecondMile.value, calculateDesiredMeetStatistic(MeetSplitsOption.FirstToSecondMile, listOf(it.second))),
                            NamedStatistic(MeetSplitsOption.SecondToThirdMile.value, calculateDesiredMeetStatistic(MeetSplitsOption.SecondToThirdMile, listOf(it.second))),
                            NamedStatistic(MeetSplitsOption.Combined.value, calculateDesiredMeetStatistic(MeetSplitsOption.Combined, listOf(it.second))),
                            NamedStatistic(MeetSplitsOption.Spread.value, calculateDesiredMeetStatistic(MeetSplitsOption.Spread, listOf(it.second))))
                }

        return splits

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
                val prPace = personalRecordService.getPRsByNameBeforeTargetDate(it.first.name, false, meets[0].date.subtractDay())
                        .firstOrNull()?.pr?.firstOrNull()?.time?.getPacePerMile()
                (prPace ?: getTimeTrialFallbackPacePerMile(it.first.id, meets[0].date)) to it.second
            }
                    .filter { it.first != null }
                    .map {
                        Triple(it.second.mileOne.calculateSecondsFrom() / it.first!!,
                                it.second.mileTwo.calculateSecondsFrom() / it.first!!,
                                it.second.mileThree.calculateSecondsFrom() / it.first!!)
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

    /**
     * Compares mile-split pacing (as % of PR pace, with time-trial fallback for runners with no prior PR)
     * across an arbitrary set of meets/seasons against one designated base meet. Powers /xc/meetSplit/paceComparison.
     */
    fun getPaceComparison(baseSeason: String, baseMeetName: String, compareKeys: List<Pair<String, String>>, paceAnchor: String, adjustForHeat: Boolean = false, adjustForGrade: Boolean = false): PaceComparisonResponse {

        val allKeys = listOf(baseSeason to baseMeetName) + compareKeys

        val candidateMeets = allKeys.mapNotNull { (season, meetName) ->
            meetRepository.findByNameAndDateBetween(meetName, Date.valueOf("$season-01-01"), Date.valueOf("$season-12-31")).firstOrNull()
        }
        val heatAdjustmentApplied = heatAdjustmentFeasibleFor(candidateMeets, adjustForHeat)
        val gradeAdjustmentApplied = gradeAdjustmentFeasibleFor(candidateMeets, adjustForGrade)

        val meetResults = allKeys.map { (season, meetName) ->
            val startDate = Date.valueOf("$season-01-01")
            val endDate = Date.valueOf("$season-12-31")
            val (meet, comparisons) = getRunnerPaceComparisonsForMeet(meetName, startDate, endDate, paceAnchor, heatAdjustmentApplied, gradeAdjustmentApplied)
            MeetPaceComparisonKeyResult(season, meetName, meet, comparisons)
        }.filter { it.meet != null }

        if (meetResults.isEmpty()) {
            return PaceComparisonResponse(paceAnchor, emptyList(), emptyList(), heatAdjustmentApplied, gradeAdjustmentApplied)
        }

        val baseMile1 = mileArray(meetResults.first().comparisons) { it.mileOnePercent }
        val baseMile2 = mileArray(meetResults.first().comparisons) { it.mileTwoPercent }
        val baseMile3 = mileArray(meetResults.first().comparisons) { it.mileThreePercent }

        val meetDTOs = meetResults.mapIndexed { index, result ->
            val meet = result.meet!!

            val mile1 = mileArray(result.comparisons) { it.mileOnePercent }
            val mile2 = mileArray(result.comparisons) { it.mileTwoPercent }
            val mile3 = mileArray(result.comparisons) { it.mileThreePercent }

            val splitStats = listOf(
                    mileSplitStat(1, mile1),
                    mileSplitStat(2, mile2),
                    mileSplitStat(3, mile3)
            )

            val avgAcrossMiles = listOf(mile1, mile2, mile3)
                    .filter { it.isNotEmpty() }
                    .map { it.average() }
                    .let { if (it.isEmpty()) 0.0 else it.average() }

            val paceSourceBreakdown = paceSourceBreakdownFor(result.comparisons)

            val rawSplits = meetMileSplitRepository.findByMeetId(meet.id)
            val avgSplitDiffs = SplitDifferenceSecondsDTO(
                    firstToSecond = rawSplits.calculateAverageFirstToSecondMileDifference(),
                    secondToThird = rawSplits.calculateAverageSecondToThirdMileDifference(),
                    spread = rawSplits.calculateAverageSpread()
            )

            val isBase = index == 0

            val vsBase = if (isBase) {
                null
            } else {
                VsBaseComparisonDTO(listOf(
                        significanceFor(1, baseMile1, mile1),
                        significanceFor(2, baseMile2, mile2),
                        significanceFor(3, baseMile3, mile3)
                ))
            }

            MeetPaceComparisonDTO(
                    season = result.season,
                    meetName = result.meetName,
                    date = meet.date,
                    runnerCount = result.comparisons.size,
                    paceSourceBreakdown = paceSourceBreakdown,
                    splits = splitStats,
                    avgPercentOfPaceAllMiles = avgAcrossMiles.round(4),
                    avgSplitDifferenceSeconds = avgSplitDiffs,
                    isBase = isBase,
                    vsBase = vsBase
            )
        }

        val ranking = meetDTOs.sortedBy { it.avgPercentOfPaceAllMiles }
                .mapIndexed { index, dto -> MeetPaceRankingDTO(dto.season, dto.meetName, index + 1, dto.avgPercentOfPaceAllMiles) }

        return PaceComparisonResponse(paceAnchor, meetDTOs, ranking, heatAdjustmentApplied, gradeAdjustmentApplied)
    }

    /**
     * Season-wide counterpart to [getPaceComparison]: aggregates every meet within each season (rather than
     * one designated meet) into a season-level pace summary, plus a week-by-week breakdown where week 1 is the
     * week of each season's first meet, so equivalent weeks-into-season line up across seasons of different length.
     * Powers /xc/meetSplit/seasonPaceComparison.
     */
    fun getSeasonPaceComparison(baseSeason: String, compareSeasons: List<String>, paceAnchor: String, adjustForHeat: Boolean = false, adjustForGrade: Boolean = false): SeasonPaceComparisonResponse {

        val allSeasons = listOf(baseSeason) + compareSeasons

        val seasonResults = allSeasons.mapNotNull { season ->
            // XC season runs roughly late-May through year end; this excludes the same team's spring track meets,
            // matching the boundary RunnerProfileAsyncHelper uses elsewhere for XC vs. track date ranges.
            val startDate = getFirstDayOfGivenYear(season).addDays(150)
            val endDate = getLastDayOfGivenYear(season)
            val meets = meetRepository.findByDateBetween(startDate, endDate).sortedBy { it.date }
            if (meets.isEmpty()) null else season to meets
        }

        if (seasonResults.isEmpty()) {
            return SeasonPaceComparisonResponse(paceAnchor, emptyList(), emptyList())
        }

        val heatAdjustmentApplied = heatAdjustmentFeasibleFor(seasonResults.flatMap { it.second }, adjustForHeat)
        val gradeAdjustmentApplied = gradeAdjustmentFeasibleFor(seasonResults.flatMap { it.second }, adjustForGrade)

        val seasonWeekData = seasonResults.mapNotNull { (season, meets) ->
            // Drop meets with no recorded split data so week 1 anchors to each season's first real race
            // (aligning seasons that open with a non-scoring intrasquad) and empty weeks don't plot as 0%.
            val meetsWithData = meets
                    .map { it to computeComparisonsForMeet(it, paceAnchor, heatAdjustmentApplied, gradeAdjustmentApplied) }
                    .filter { it.second.isNotEmpty() }
            if (meetsWithData.isEmpty()) return@mapNotNull null
            val seasonStart = meetsWithData.first().first.date
            val weekBuckets = meetsWithData.groupBy { weekOfSeason(seasonStart, it.first.date) }
                    .toSortedMap()
                    .map { (week, weekPairs) ->
                        Triple(week, weekPairs.map { it.first }, weekPairs.flatMap { it.second })
                    }
            Triple(season, meetsWithData.map { it.first }, weekBuckets)
        }

        if (seasonWeekData.isEmpty()) {
            return SeasonPaceComparisonResponse(paceAnchor, emptyList(), emptyList(), heatAdjustmentApplied, gradeAdjustmentApplied)
        }

        val baseComparisons = seasonWeekData.first().third.flatMap { it.third }
        val baseMile1 = mileArray(baseComparisons) { it.mileOnePercent }
        val baseMile2 = mileArray(baseComparisons) { it.mileTwoPercent }
        val baseMile3 = mileArray(baseComparisons) { it.mileThreePercent }

        val baseWeekMiles = seasonWeekData.first().third.associate { (week, _, comparisons) ->
            week to Triple(
                    mileArray(comparisons) { it.mileOnePercent },
                    mileArray(comparisons) { it.mileTwoPercent },
                    mileArray(comparisons) { it.mileThreePercent }
            )
        }

        val seasonDTOs = seasonWeekData.mapIndexed { index, (season, meets, weekBuckets) ->
            val isBase = index == 0
            val comparisons = weekBuckets.flatMap { it.third }

            val weekDTOs = weekBuckets.map { (week, weekMeets, weekComparisons) ->
                val mile1 = mileArray(weekComparisons) { it.mileOnePercent }
                val mile2 = mileArray(weekComparisons) { it.mileTwoPercent }
                val mile3 = mileArray(weekComparisons) { it.mileThreePercent }

                val vsBase = if (isBase) null else {
                    val baseMiles = baseWeekMiles[week]
                    if (baseMiles == null) null else VsBaseComparisonDTO(listOf(
                            significanceFor(1, baseMiles.first, mile1),
                            significanceFor(2, baseMiles.second, mile2),
                            significanceFor(3, baseMiles.third, mile3)
                    ))
                }

                WeekPaceComparisonDTO(
                        week = week,
                        meetNames = weekMeets.map { it.name },
                        startDate = weekMeets.minByOrNull { it.date }!!.date,
                        runnerCount = weekComparisons.size,
                        paceSourceBreakdown = paceSourceBreakdownFor(weekComparisons),
                        splits = listOf(mileSplitStat(1, mile1), mileSplitStat(2, mile2), mileSplitStat(3, mile3)),
                        avgPercentOfPaceAllMiles = averageAcrossMiles(mile1, mile2, mile3).round(4),
                        vsBase = vsBase
                )
            }

            val mile1 = mileArray(comparisons) { it.mileOnePercent }
            val mile2 = mileArray(comparisons) { it.mileTwoPercent }
            val mile3 = mileArray(comparisons) { it.mileThreePercent }

            val vsBase = if (isBase) null else VsBaseComparisonDTO(listOf(
                    significanceFor(1, baseMile1, mile1),
                    significanceFor(2, baseMile2, mile2),
                    significanceFor(3, baseMile3, mile3)
            ))

            SeasonPaceComparisonDTO(
                    season = season,
                    meetCount = meets.size,
                    runnerCount = comparisons.size,
                    paceSourceBreakdown = paceSourceBreakdownFor(comparisons),
                    splits = listOf(mileSplitStat(1, mile1), mileSplitStat(2, mile2), mileSplitStat(3, mile3)),
                    avgPercentOfPaceAllMiles = averageAcrossMiles(mile1, mile2, mile3).round(4),
                    isBase = isBase,
                    vsBase = vsBase,
                    weeks = weekDTOs
            )
        }

        val ranking = seasonDTOs.sortedBy { it.avgPercentOfPaceAllMiles }
                .mapIndexed { index, dto -> SeasonPaceRankingDTO(dto.season, index + 1, dto.avgPercentOfPaceAllMiles) }

        return SeasonPaceComparisonResponse(paceAnchor, seasonDTOs, ranking, heatAdjustmentApplied, gradeAdjustmentApplied)
    }

    private fun weekOfSeason(seasonStart: Date, meetDate: Date): Int {
        val diffDays = (meetDate.time - seasonStart.time) / (1000L * 60 * 60 * 24)
        return (diffDays / 7).toInt() + 1
    }

    private fun paceSourceBreakdownFor(comparisons: List<RunnerPaceComparison>): PaceSourceBreakdownDTO {
        return PaceSourceBreakdownDTO(
                sb = comparisons.count { it.paceSource == PaceSource.SB },
                pr = comparisons.count { it.paceSource == PaceSource.PR },
                timeTrial = comparisons.count { it.paceSource == PaceSource.TIME_TRIAL },
                none = comparisons.count { it.paceSource == PaceSource.NONE }
        )
    }

    private fun averageAcrossMiles(vararg mileArrays: DoubleArray): Double {
        return mileArrays.filter { it.isNotEmpty() }
                .map { it.average() }
                .let { if (it.isEmpty()) 0.0 else it.average() }
    }

    private fun getRunnerPaceComparisonsForMeet(filterMeet: String, startDate: Date, endDate: Date, paceAnchor: String, adjustForHeat: Boolean = false, adjustForGrade: Boolean = false): Pair<Meet?, List<RunnerPaceComparison>> {
        val meets = meetRepository.findByNameAndDateBetween(filterMeet, startDate, endDate)
        val meet = meets.firstOrNull() ?: return null to emptyList()
        return meet to computeComparisonsForMeet(meet, paceAnchor, adjustForHeat, adjustForGrade)
    }

    private fun computeComparisonsForMeet(meet: Meet, paceAnchor: String, adjustForHeat: Boolean = false, adjustForGrade: Boolean = false): List<RunnerPaceComparison> {
        val splits = meetMileSplitRepository.findByMeetId(meet.id)
        val resolveWeather = weatherResolverFor(meet, adjustForHeat)
        val courseCostFactor = courseCostFactorFor(meet, adjustForGrade)

        return splits.map { split ->
            val runner = runnerRepository.findById(split.runnerId).get()
            val (pace, source) = resolvePaceAnchor(runner.name, runner.id, meet.date, paceAnchor)

            if (pace == null) {
                RunnerPaceComparison(PaceSource.NONE, null, null, null)
            } else {
                val weather = resolveWeather(split.runnerId)
                RunnerPaceComparison(source,
                        normalizedSeconds(split.mileOne, weather, courseCostFactor) / pace,
                        normalizedSeconds(split.mileTwo, weather, courseCostFactor) / pace,
                        normalizedSeconds(split.mileThree, weather, courseCostFactor) / pace)
            }
        }
    }

    private fun normalizedSeconds(splitTime: String, weather: ResolvedWeather?, courseCostFactor: Double? = null): Double {
        var seconds = splitTime.calculateSecondsFrom()
        if (weather != null) {
            seconds = heatPaceAdjustmentService.normalizeResultTimeForHeat(seconds, weather.temperatureC, weather.humidityPct)
        }
        if (courseCostFactor != null) {
            seconds = courseGradeAdjustmentService.normalizeResultTimeForGrade(seconds, courseCostFactor)
        }
        return seconds
    }

    /**
     * Builds a per-runner weather lookup for a meet, used to normalize actual splits to their
     * flat/ideal-condition equivalent before comparing against goal pace -- so a hot day doesn't
     * read as a pacing miss. Resolves per-division weather when the runner's result has one (an
     * early Open race and a late Varsity race at the same meet can see real different conditions),
     * falling back to the meet-level lookup. Returns a function that always returns null (no
     * adjustment) when [adjustForHeat] is false or the meet has no venue coordinates.
     */
    /** Meet-level feasibility check: can we resolve *any* weather for this meet at all? Used to decide
     *  whether to trust a heat-adjusted comparison across a whole set of meets -- see [heatAdjustmentFeasibleFor]. */
    private fun canResolveWeather(meet: Meet): Boolean {
        val venue = meet.venueId?.let { venueRepository.findById(it).orElse(null) } ?: return false
        return eventWeatherResolver.resolve(EventWeatherResolver.EVENT_TYPE_MEET, meet.id, venue, meet.date, meet.startTime) != null
    }

    /**
     * Only ever returns true if heat adjustment was requested AND every meet in the set resolves
     * weather -- comparing a heat-adjusted meet against a raw one would make the %-of-pace numbers
     * and significance tests meaningless, so this is all-or-nothing rather than per-meet.
     */
    private fun heatAdjustmentFeasibleFor(meets: List<Meet>, adjustForHeat: Boolean): Boolean {
        return adjustForHeat && meets.isNotEmpty() && meets.all { canResolveWeather(it) }
    }

    /** Meet-level feasibility check: does this meet have a course profile with a computed cost factor? */
    private fun canResolveCourseProfile(meet: Meet): Boolean {
        val courseProfileId = meet.courseProfileId ?: return false
        val profile = venueCourseProfileRepository.findById(courseProfileId).orElse(null) ?: return false
        return profile.courseCostFactor != null
    }

    /**
     * Same all-or-nothing semantics as [heatAdjustmentFeasibleFor], independent of the heat flag --
     * comparing a grade-adjusted meet against a raw one would make the comparison meaningless.
     */
    private fun gradeAdjustmentFeasibleFor(meets: List<Meet>, adjustForGrade: Boolean): Boolean {
        return adjustForGrade && meets.isNotEmpty() && meets.all { canResolveCourseProfile(it) }
    }

    private fun courseCostFactorFor(meet: Meet, adjustForGrade: Boolean): Double? {
        if (!adjustForGrade) return null
        val courseProfileId = meet.courseProfileId ?: return null
        return venueCourseProfileRepository.findById(courseProfileId).orElse(null)?.courseCostFactor
    }

    private fun weatherResolverFor(meet: Meet, adjustForHeat: Boolean): (Int) -> ResolvedWeather? {
        if (!adjustForHeat) return { null }
        val venue = meet.venueId?.let { venueRepository.findById(it).orElse(null) } ?: return { null }

        val divisionIdByRunnerId = meetPerformanceService.getResultsForMeet(meet.id).associate { it.runnerId to it.divisionId }
        val weatherByDivisionId = mutableMapOf<Int?, ResolvedWeather?>()

        return { runnerId ->
            val divisionId = divisionIdByRunnerId[runnerId]
            weatherByDivisionId.getOrPut(divisionId) {
                val division = divisionId?.let { meetDivisionRepository.findById(it).orElse(null) }
                if (division != null) {
                    eventWeatherResolver.resolve(EventWeatherResolver.EVENT_TYPE_MEET_DIVISION, division.id, venue, meet.date, division.startTime ?: meet.startTime)
                } else {
                    eventWeatherResolver.resolve(EventWeatherResolver.EVENT_TYPE_MEET, meet.id, venue, meet.date, meet.startTime)
                }
            }
        }
    }

    /**
     * Resolves the goal pace-per-mile a runner's splits are measured against, plus which source it came from.
     * SB mode prefers this-season-before-meet season best, falling back to PR then time trial; PR mode keeps the
     * original PR-then-time-trial waterfall. Fallbacks are evaluated lazily so PR mode issues no SB query.
     */
    private fun resolvePaceAnchor(runnerName: String, runnerId: Int, meetDate: Date, paceAnchor: String): Pair<Double?, PaceSource> {
        val prPace: () -> Double? = {
            personalRecordService.getPRsByNameBeforeTargetDate(runnerName, false, meetDate.subtractDay())
                    .firstOrNull()?.pr?.firstOrNull()?.time?.getPacePerMile()
        }

        if (paceAnchor.equals("SB", true)) {
            val sbPace = seasonBestService.getSeasonBestBeforeDate(runnerId, meetDate)?.time?.getPacePerMile()
            if (sbPace != null) return sbPace to PaceSource.SB
        }

        prPace()?.let { return it to PaceSource.PR }
        getTimeTrialFallbackPacePerMile(runnerId, meetDate)?.let { return it to PaceSource.TIME_TRIAL }
        return null to PaceSource.NONE
    }

    private fun mileArray(comparisons: List<RunnerPaceComparison>, selector: (RunnerPaceComparison) -> Double?): DoubleArray {
        return comparisons.mapNotNull(selector).toDoubleArray()
    }

    private fun mileSplitStat(mile: Int, values: DoubleArray): MileSplitStatDTO {
        if (values.isEmpty()) {
            return MileSplitStatDTO(mile, 0.0, 0.0, 0)
        }
        return MileSplitStatDTO(mile, values.average().round(4), values.toList().standardDeviation().round(4), values.size)
    }

    private fun significanceFor(mile: Int, base: DoubleArray, compare: DoubleArray): MileSplitSignificanceDTO {
        if (base.size < 2 || compare.size < 2) {
            return MileSplitSignificanceDTO(mile, 1.0, false)
        }
        val p = TestUtils.tTest(base, compare).round(4)
        return MileSplitSignificanceDTO(mile, p, p < 0.05)
    }

}