package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statisitcs.model.*
import com.terkula.uaxctf.statistics.controller.MeetPerformanceController
import com.terkula.uaxctf.statistics.dto.*
import com.terkula.uaxctf.statistics.exception.RunnerNotFoundByPartialNameException
import com.terkula.uaxctf.statistics.repository.MeetRepository
import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.statistics.request.SortingMethodContainer
import com.terkula.uaxctf.training.dto.RunnerWorkoutResultsDTO
import com.terkula.uaxctf.training.service.WorkoutResultService
import com.terkula.uaxctf.util.TimeUtilities
import com.terkula.uaxctf.util.toMinuteSecondString
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.AsyncResult
import org.springframework.stereotype.Component
import java.sql.Date
import java.time.Instant
import java.util.concurrent.Future

@Component
open class RunnerProfileAsyncHelper (@field:Autowired
                                internal var runnerRepository: RunnerRepository,
                                     @field:Autowired
                                internal var seasonBestService: SeasonBestService,
                                     @field:Autowired
                                internal var personalRecordService: PersonalRecordService,
                                     @field: Autowired
                                internal var meetRepository: MeetRepository,
                                     @field: Autowired
                                internal var goalService: XcGoalService,
                                     @field: Autowired
                                internal var meetMileSplitService: MeetMileSplitService,
                                     @field: Autowired
                                internal val runnerConsistencyRankService: ConsistencyRankService,
                                     @field: Autowired
                                internal val timeTrialProgressionService: TimeTrialService,
                                     @field: Autowired
                                internal val workoutResultService: WorkoutResultService,
                                     @field: Autowired
                                internal val meetPerformanceService: MeetPerformanceService) {


//    fun buildRunnerProfile(name: String): RunnerProfileDTO {
//
//        // todo can you find a way to speed this up? takes about 15s... can make calls async? limit queries to DB where possible?
//
//        val runner =
//                try {
//                    runnerRepository.findByNameContaining(name).first()
//                } catch (e: Exception) {
//                    throw RunnerNotFoundByPartialNameException("No runner matching the given name: $name")
//                }
//
//        val startDate = Date.valueOf(MeetPerformanceController.CURRENT_YEAR + "-01-01")
//        val endDate = Date.valueOf(MeetPerformanceController.CURRENT_YEAR + "-12-31")
//
//        val seasonBests = seasonBestService.getSeasonBestsByName(name, listOf(startDate to endDate), false)
//        var seasonBest: RunnerMeetSplitDTO? = null
//        if (seasonBests.isNotEmpty()) {
//            if (seasonBests.first().seasonBest.isNotEmpty()) {
//                seasonBest = RunnerMeetSplitDTO(seasonBests.first().seasonBest.first(), null)
//            }
//        }
//
//        val meetSplits = meetMileSplitService.getAllMeetMileSplitsForRunner(name, startDate, endDate).mileSplits
//
//        val mostConsistentRace = meetSplits.sortedBy { it.meetSplitsDTO!!.toMeetSplit().calculateSpread() }
//                .firstOrNull()
//
//        val seasonBestSplits = getSplitsForMeetPerformances(seasonBest?.meetPerformanceDTO, runner)
//
//        val prs = personalRecordService.getPRsByName(name, false)
//        var pr: MeetPerformanceDTO? = null
//
//        if (prs.isNotEmpty()) {
//            if (prs.first().pr.isNotEmpty())
//                pr = prs.first().pr.first()
//        }
//
//        var prSplits: RunnerMeetSplitDTO? = null
//
//        if (pr != null) {
//            prSplits = getSplitsForMeetPerformances(pr, runner)
//        }
//
//        val goals: List<String?> = goalService.getRunnersGoalForSeason(name, MeetPerformanceController.CURRENT_YEAR).first().times
//
//        val workouts = workoutResultService.getWorkoutsForRunner(startDate, endDate, name, 0, "", "").workouts
//        var lastWorkout: RunnerWorkoutResultsDTO? = null
//        if (workouts.isNotEmpty()) {
//            lastWorkout = workouts.sortedBy { it.workout.date }.first()
//        }
//
//        val workoutConsistency =
//                runnerConsistencyRankService.getRunnersOrderedByMostConsistentWorkouts(startDate, endDate)
//        val workoutRank = workoutConsistency
//                .filter { it.runner.id == runner.id }
//                .map {
//                    ValuedRank("Workout Rank", it.consistencyRank.rank, it.consistencyRank.consistencyValue.toMinuteSecondString())
//                }
//                .firstOrNull()
//
//        val raceConsistency = runnerConsistencyRankService.getRunnersOrderedByMostConsistentRaces(startDate, endDate)
//        val raceRank = raceConsistency
//                .filter { it.runner.id == runner.id }
//                .map {
//                    ValuedRank("Race Rank", it.consistencyRank.rank, it.consistencyRank.consistencyValue.toMinuteSecondString())
//                }
//                .firstOrNull()
//
//        val totalConsistency = runnerConsistencyRankService.getRunnersOrderedMostConsistent(startDate, endDate, 1.0)
//        val combinedRank = totalConsistency
//                .filter { it.runner.id == runner.id }
//                .map { ValuedRank("Workout And Race Consistency Rank", it.consistencyRank.rank, it.consistencyRank.consistencyValue.toMinuteSecondString()) }
//                .firstOrNull()
//
//        val progressionRank = timeTrialProgressionService.getRankedProgressionSinceTimeTrial(startDate, endDate, false)
//                .filter { it.runner.id == runner.id }
//                .map { ValuedRank("Progression Rank", it.rank, it.improvement) }
//                .firstOrNull()
//
//
//        ////////// build season best rank //////////
//
//        val allSeasonBests = seasonBestService.getAllSeasonBests(startDate, endDate, false).map { it.runner to it.seasonBest }
//                .filter {
//                    it.second.isNotEmpty()
//                }
//                .map {
//                    it.first.id to it.second.first().time
//                }.toMutableList()
//
//        // get time trial results as fall backs for runners with no season best yet
//
//        val allAdjustedTimeTrials = timeTrialProgressionService.getAllAdjustedTimeTrials(startDate, endDate)
//                .map { it.runnerId to it.time }
//                .filter {
//                    !allSeasonBests.map { sb -> sb.first }.contains(it.first)
//                }
//
//        allSeasonBests.addAll(allAdjustedTimeTrials)
//
//        val timeRank = allSeasonBests.mapIndexed { index, it ->
//            Triple(index + 1, it.first, it.second)
//        }.sortedBy { it.third }.filter { it.second == runner.id }
//                .map { NamedRank("Season Best", it.first) }.firstOrNull()
//
//
//        /////// meet info for runner from last year //////
//
//        val upcomingMeets = meetRepository.findByDateBetween(startDate, endDate).filter {
//            it.date.after(Date.from(Instant.now()))
//        }.sortedByDescending { it.date }
//
//        var upcomingMeet: Meet? = null
//        if (upcomingMeets.isNotEmpty()) {
//            upcomingMeet = upcomingMeets.first()
//        }
//
//        var lastYearPerformanceAtUpcomingMeet: MeetPerformanceDTO?
//        var upComingMeetSplitsLastYear: RunnerMeetSplitDTO? = null
//
//        if (upcomingMeet != null) {
//            val lastYearStart = Date.valueOf((MeetPerformanceController.CURRENT_YEAR.toInt() - 1).toString() + "-01-01")
//            val lastYearEnd = Date.valueOf((MeetPerformanceController.CURRENT_YEAR.toInt() - 1).toString() + "-12-31")
//            val performances = meetPerformanceService.getMeetPerformancesForRunnerWithNameContaining(name, lastYearStart, lastYearEnd, SortingMethodContainer.RECENT_DATE, 20, false)
//                    .map {
//                        it.performance
//                    }.flatten()
//
//            lastYearPerformanceAtUpcomingMeet = performances.filter {
//                it.meetName.equals(upcomingMeet.name, ignoreCase = true)
//            }.firstOrNull()
//
//
//            upComingMeetSplitsLastYear = getSplitsForMeetPerformances(lastYearPerformanceAtUpcomingMeet, runner)
//
//        }
//
//        return RunnerProfileDTO(runner, goals, seasonBestSplits, prSplits, mostConsistentRace, lastWorkout,
//                workoutRank, raceRank, combinedRank, progressionRank, timeRank, upComingMeetSplitsLastYear)
//
//    }

    fun getSplitsForMeetPerformances(meetPerformance: MeetPerformanceDTO?, runner: Runner): RunnerMeetSplitDTO? {

        var splits: RunnerMeetSplitDTO? = null

        if (meetPerformance != null) {

            val startDate = TimeUtilities.Companion.getFirstDayOfYear()
            val endDate = TimeUtilities.Companion.getLastDayOfYear()

            val meetSplits = meetMileSplitService.getAllMeetMileSplitsForRunner(runner.name, startDate, endDate).mileSplits
            splits = meetSplits.firstOrNull { it.meetPerformanceDTO.meetName == meetPerformance.meetName }

            if (splits == null) {
                splits = RunnerMeetSplitDTO(meetPerformance, null)
            }
        }

        return splits

    }

    @Async
    open fun getWorkoutConsistency(startDate: Date, endDate: Date): Future<List<RunnerConsistencyDTO>> {
        return AsyncResult(runnerConsistencyRankService.getRunnersOrderedByMostConsistentWorkouts(startDate, endDate))
    }


    @Async
    open fun getRaceConsistency(startDate: Date, endDate: Date): Future<List<RunnerConsistencyDTO>> {
        return AsyncResult(runnerConsistencyRankService.getRunnersOrderedByMostConsistentRaces(startDate, endDate))
    }

    @Async
    open fun getTotalConsistency(startDate: Date, endDate: Date, weight: Double): Future<List<RunnerConsistencyDTO>> {
        return AsyncResult(runnerConsistencyRankService.getRunnersOrderedMostConsistent(startDate, endDate, weight))
    }

    @Async
    open fun getTimeTrialProgression(startDate: Date, endDate: Date): Future<List<TimeTrialImprovementDTO>> {
        return AsyncResult(timeTrialProgressionService.getRankedProgressionSinceTimeTrial(startDate, endDate, true))
    }

    @Async
    open fun getMeetPerformancesLastYear(name: String, startDate: Date, endDate: Date,
                                           sortingMethodContainer: SortingMethodContainer, count: Int): Future<List<RunnerPerformanceDTO>> {
        return AsyncResult(meetPerformanceService.getMeetPerformancesForRunnerWithNameContaining(name, startDate, endDate,
                sortingMethodContainer, count, false))
    }

    @Async
    open fun getAllSeasonBests(startDate: Date, endDate: Date): Future<List<SeasonBestDTO>> {
        return AsyncResult(seasonBestService.getAllSeasonBests(startDate, endDate, false))
    }

    @Async
    open fun getTimeTrials(startDate: Date, endDate: Date): Future<List<TimeTrial>> {
        return AsyncResult(timeTrialProgressionService.getAllAdjustedTimeTrials(startDate, endDate))
    }

    @Async
    open fun getGoalForRunner(name: String, year: String): Future<RunnerGoalDTO> {
        return AsyncResult(goalService.getRunnersGoalForSeason(name, year))
    }

    @Async
    open fun getAllMeetSplits(name: String, startDate: Date, endDate: Date): Future<List<RunnerMeetSplitDTO>>  {
        return AsyncResult(meetMileSplitService.getAllMeetMileSplitsForRunner(name, startDate, endDate).mileSplits)
    }


}