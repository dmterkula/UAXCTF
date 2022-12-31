package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statisitcs.model.*
import com.terkula.uaxctf.statistics.controller.MeetPerformanceController
import com.terkula.uaxctf.statistics.dto.*
import com.terkula.uaxctf.statistics.exception.RunnerNotFoundByPartialNameException
import com.terkula.uaxctf.statistics.repository.MeetRepository
import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.statistics.request.SortingMethodContainer
import com.terkula.uaxctf.training.dto.RunnerWorkoutResultsDTO
import com.terkula.uaxctf.training.model.DateRangeRunSummaryDTO
import com.terkula.uaxctf.training.model.TrainingRunResults
import com.terkula.uaxctf.training.response.RankedRunnerDistanceRunDTO
import com.terkula.uaxctf.training.response.RunnerWorkoutResultResponse
import com.terkula.uaxctf.training.service.TrainingRunsService
import com.terkula.uaxctf.training.service.WorkoutResultService
import com.terkula.uaxctf.training.service.WorkoutSplitService
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
open class RunnerProfileAsyncHelper (
    var runnerRepository: RunnerRepository,
    var seasonBestService: SeasonBestService,
    var personalRecordService: PersonalRecordService,
    var meetRepository: MeetRepository,
    var goalService: XcGoalService,
    var meetMileSplitService: MeetMileSplitService,
    val runnerConsistencyRankService: ConsistencyRankService,
    val timeTrialProgressionService: TimeTrialService,
    val meetPerformanceService: MeetPerformanceService,
    val leaderBoardService: LeaderBoardService,
    val trainingRunsService: TrainingRunsService,
    val workoutSplitService: WorkoutSplitService,
) {


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



    // new

    @Async
    open fun getPRLeaderboard(): Future<List<RankedPRDTO>>  {
        return AsyncResult(leaderBoardService.getPRLeaderBoard(pageSize = 1000))
    }

    @Async
    open fun getSBLeaderboard(season: String): Future<List<RankedSeasonBestDTO>>  {
        return AsyncResult(leaderBoardService.getSeasonBestLeaderBoard(season))
    }

    @Async
    open fun getRaceConsistencyLeaderboard(season: String): Future<List<RankedRunnerConsistencyDTO>>  {
        return AsyncResult(leaderBoardService.getRaceConsistentRankLeaderBoard(season))
    }

    @Async
    open fun getDistanceRunLeaderBoard(season: String): Future<List<RankedRunnerDistanceRunDTO>>  {
        return AsyncResult(leaderBoardService.getDistanceRunRankLeaderBoard(season))
    }

    @Async
    open fun getTimeTrialProgressionLeaderboard(season: String): Future<List<TimeTrialImprovementDTO>>  {
        return AsyncResult(leaderBoardService.getTimeTrialProgressionLeaderBoard(season))
    }

    @Async
    open fun getTrainingRuns(runnerId: Int, season: String): Future<TrainingRunResults>  {
        return AsyncResult(trainingRunsService.getARunnersTrainingRunsWithinDates(runnerId, TimeUtilities.getFirstDayOfGivenYear(season), TimeUtilities.getLastDayOfGivenYear(season)))
    }

    @Async
    open fun getWorkoutResults(runnerId: Int, season: String): Future<List<RunnerWorkoutResultResponse>>  {
        return AsyncResult(workoutSplitService.getAllARunnersWorkoutResults(runnerId, season))
    }

    @Async
    open fun getGoalForRunner(runnerId: Int, year: String): Future<RunnerGoalDTO> {
        return AsyncResult(goalService.getRunnersGoalForSeason(runnerId, year))
    }

    @Async
    open fun getMeetResults(runnerId: Int, season: String,
                                         sortingMethodContainer: SortingMethodContainer, count: Int): Future<List<RunnerPerformanceDTO>> {
        return AsyncResult(meetPerformanceService.getMeetPerformancesForRunner(runnerId, TimeUtilities.getFirstDayOfGivenYear(season), TimeUtilities.getLastDayOfGivenYear(season),
                sortingMethodContainer, count, false))
    }

    @Async
    open fun getTrainingRunSummary(runnerId: Int, season: String): Future<List<DateRangeRunSummaryDTO>> {
        return AsyncResult(trainingRunsService.getTotalDistancePerWeek(season, runnerId))
    }



}