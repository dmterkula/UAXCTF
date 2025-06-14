package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statisitcs.model.*
import com.terkula.uaxctf.statisitcs.model.track.TrackMeetPerformance
import com.terkula.uaxctf.statisitcs.model.track.TrackMeetPerformanceDTO
import com.terkula.uaxctf.statistics.dto.*
import com.terkula.uaxctf.statistics.dto.leaderboard.RankedSeasonBestDTO
import com.terkula.uaxctf.statistics.dto.leaderboard.SeasonBestDTO
import com.terkula.uaxctf.statistics.dto.track.TrackPerformancesDTO
import com.terkula.uaxctf.statistics.repository.MeetRepository
import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.statistics.request.SortingMethodContainer
import com.terkula.uaxctf.statistics.response.achievement.RunnerAchievementsDTO
import com.terkula.uaxctf.statistics.service.track.TrackMeetPerformanceService
import com.terkula.uaxctf.statistics.service.track.TrackPRService
import com.terkula.uaxctf.statistics.service.track.TrackSBService
import com.terkula.uaxctf.training.model.DateRangeRunSummaryDTO
import com.terkula.uaxctf.training.model.TrainingRunResults
import com.terkula.uaxctf.training.response.RankedRunnerDistanceRunDTO
import com.terkula.uaxctf.training.response.RunnerWorkoutResultResponse
import com.terkula.uaxctf.training.response.crosstraining.CrossTrainingRecordProfileResponse
import com.terkula.uaxctf.training.response.runnerseasontrainingcount.RunnerSeasonTrainingCount
import com.terkula.uaxctf.training.service.*
import com.terkula.uaxctf.util.TimeUtilities
import com.terkula.uaxctf.util.subtractDay
import com.terkula.uaxctf.util.subtractDays
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.AsyncResult
import org.springframework.stereotype.Component
import java.sql.Date
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
    val achievementService: AchievementService,
    val trackMeetPerformanceService: TrackMeetPerformanceService,
    val trackPRService: TrackPRService,
    val trackSBService: TrackSBService,
    val crossTrainingService: CrossTrainingService,
    val summerTrainingAwardService: SummerTrainingAwardService
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
    open fun getSummerTrainingAwardStatus(runnerId: Int, season: String, year: String, team: String): Future<RunnerSeasonTrainingCount?> {
        return AsyncResult(summerTrainingAwardService.getSeasonTrainingCountsForRunner(runnerId, season, year, team))
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
    open fun getTrackMeetPerformances(runnerId: Int, season: String): Future<List<TrackMeetPerformanceDTO>> {
        return AsyncResult(trackMeetPerformanceService.getTrackMeetResultsForRunner(runnerId, season))
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
    open fun getDistanceRunLeaderBoard(season: String, type: String): Future<List<RankedRunnerDistanceRunDTO>>  {
        return AsyncResult(leaderBoardService.getDistanceRunRankLeaderBoard(season, type))
    }

    @Async
    open fun getTimeTrialProgressionLeaderboard(season: String): Future<List<TimeTrialImprovementDTO>>  {
        return AsyncResult(leaderBoardService.getTimeTrialProgressionLeaderBoard(season))
    }

    @Async
    open fun getTrainingRuns(runnerId: Int, season: String, type: String): Future<TrainingRunResults>  {
        if (type.equals("xc", ignoreCase = true)) {
            return AsyncResult(trainingRunsService.getARunnersTrainingRunsByTypeWithinDates(runnerId, TimeUtilities.getFirstDayOfGivenYear(season), TimeUtilities.getLastDayOfGivenYear(season), type))
        } else {
            val startDate = TimeUtilities.getFirstDayOfGivenYear(season).subtractDays(90) // start in roughly october of previous year looking for track records
            val endDate = TimeUtilities.getLastDayOfGivenYear(season).subtractDays(150) // stop looking for track records in august
            return AsyncResult(trainingRunsService.getARunnersTrainingRunsByTypeWithinDates(runnerId, startDate, endDate, type))
        }
    }

    @Async
    open fun getCrossTrainingWorkouts(runnerId: Int, season: String, type: String): Future<List<CrossTrainingRecordProfileResponse>>  {
        if (type.equals("xc", ignoreCase = true)) {
            return AsyncResult(crossTrainingService.getCrossTrainingActivitiesForRunnerBetweenDatesForSeason(runnerId, TimeUtilities.getFirstDayOfGivenYear(season), TimeUtilities.getLastDayOfGivenYear(season), type))
        } else {
            val startDate = TimeUtilities.getFirstDayOfGivenYear(season).subtractDays(90) // start in roughly october of previous year looking for track records
            val endDate = TimeUtilities.getLastDayOfGivenYear(season).subtractDays(150) // stop looking for track records in august
            return AsyncResult(crossTrainingService.getCrossTrainingActivitiesForRunnerBetweenDatesForSeason(runnerId, startDate, endDate, type))
        }
    }

    @Async
    open fun getWorkoutResults(runnerId: Int, season: String, type: String): Future<List<RunnerWorkoutResultResponse>>  {
        if (type.equals("xc", ignoreCase = true)) {
            return AsyncResult(workoutSplitService.getAllARunnersWorkoutResultsBySeason(runnerId, season, TimeUtilities.getFirstDayOfGivenYear(season), TimeUtilities.getLastDayOfGivenYear(season), type))
        } else {
            val startDate = TimeUtilities.getFirstDayOfGivenYear(season).subtractDays(90) // start in roughly october of previous year looking for track records
            val endDate = TimeUtilities.getLastDayOfGivenYear(season).subtractDays(150) // stop looking for track records in august
            return AsyncResult(workoutSplitService.getAllARunnersWorkoutResultsBySeason(runnerId, season, startDate, endDate, type))
        }

    }

    @Async
    open fun getGoalForRunner(runnerId: Int, year: String, season: String): Future<RunnerGoalDTO> {
        return AsyncResult(goalService.getRunnersGoalForYearAndSeason(runnerId, year, season))
    }

    @Async
    open fun getMeetResults(runnerId: Int, season: String,
                                         sortingMethodContainer: SortingMethodContainer, count: Int): Future<List<RunnerPerformanceDTO>> {
        return AsyncResult(meetPerformanceService.getMeetPerformancesForRunner(runnerId, TimeUtilities.getFirstDayOfGivenYear(season), TimeUtilities.getLastDayOfGivenYear(season),
                sortingMethodContainer, count, false))
    }

    @Async
    open fun getTrainingRunSummary(runnerId: Int, season: String, includeWarmUps: Boolean, type: String): Future<List<DateRangeRunSummaryDTO>> {

        var startDate = TimeUtilities.getFirstDayOfGivenYear(season)
        var endDate = TimeUtilities.getLastDayOfGivenYear(season)

        if (type.equals("track", ignoreCase = true)) {
            startDate = TimeUtilities.getFirstDayOfGivenYear(season).subtractDays(90)
            endDate = TimeUtilities.getLastDayOfGivenYear(season).subtractDays(150)
        }

        return AsyncResult(trainingRunsService.getTotalDistancePerWeek(startDate, endDate, runnerId, includeWarmUps, type))
    }

    @Async
    open fun getAchievements(runnerId: Int): Future<RunnerAchievementsDTO> {
        return AsyncResult(achievementService.getRunnersAchievements(runnerId))
    }

    @Async
    open fun getTrackPRs(runnerId: Int): Future<TrackPerformancesDTO> {
        return AsyncResult(trackPRService.getARunnersPRs(runnerId, false, "", false))
    }

    @Async
    open fun getTrackSBs(runnerId: Int, season: String): Future<TrackPerformancesDTO> {
        return AsyncResult(trackSBService.getARunnersSBs(runnerId, false, "", season, false))
    }

}