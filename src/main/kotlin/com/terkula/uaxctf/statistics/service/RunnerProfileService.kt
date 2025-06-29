package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statisitcs.model.*
import com.terkula.uaxctf.statistics.controller.MeetPerformanceController
import com.terkula.uaxctf.statistics.dto.*
import com.terkula.uaxctf.statistics.exception.RunnerNotFoundByPartialNameException
import com.terkula.uaxctf.statistics.repository.MeetRepository
import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.statistics.request.SortingMethodContainer
import com.terkula.uaxctf.statistics.response.achievement.RunnerAchievementsDTO
import com.terkula.uaxctf.training.dto.RunnerWorkoutResultsDTO
import com.terkula.uaxctf.training.repository.MeetLogRepository
import com.terkula.uaxctf.training.repository.RunnerWorkoutDistanceRepository
import com.terkula.uaxctf.training.repository.RunnersTrainingRunRepository
import com.terkula.uaxctf.training.repository.WorkoutRepository
import com.terkula.uaxctf.training.service.WorkoutResultService
import com.terkula.uaxctf.util.TimeUtilities
import com.terkula.uaxctf.util.toMinuteSecondString
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.sql.Date
import java.time.Instant

@Component
class RunnerProfileService (
        @field:Autowired
        internal var runnerRepository: RunnerRepository,
        @field:Autowired
        internal var seasonBestService: SeasonBestService,
        @field:Autowired
        internal var personalRecordService: PersonalRecordService,
        @field: Autowired
        internal var meetRepository: MeetRepository,
        @field: Autowired
        internal var meetMileSplitService: MeetMileSplitService,
        @field: Autowired
        internal val workoutResultService: WorkoutResultService,
        @field: Autowired
        internal val runnerProfileAsyncHelper: RunnerProfileAsyncHelper,
        var runnersTrainingRunRepository: RunnersTrainingRunRepository,
        var meetLogRepository: MeetLogRepository,
        var runnerWorkoutDistanceRepository: RunnerWorkoutDistanceRepository
        ) {


    fun buildRunnerProfileV2(runnerId: Int, season: String, type: String, team: String, includeWarmUps: Boolean): RunnerProfileDTOV2 {
        val runner = runnerRepository.findById(runnerId).get()


        ////////// async oeprations //////////


        val prRanksFuture = runnerProfileAsyncHelper.getPRLeaderboard()
        val sbRanksFuture = runnerProfileAsyncHelper.getSBLeaderboard(season)
        val meetSplitConsistencyRanksFuture = runnerProfileAsyncHelper.getRaceConsistencyLeaderboard(season)
        val timeTrialProgressionRanksFuture = runnerProfileAsyncHelper.getTimeTrialProgressionLeaderboard(season)
        val distanceRunRanksFuture = runnerProfileAsyncHelper.getDistanceRunLeaderBoard(season, type)
        val trainingRunsFuture = runnerProfileAsyncHelper.getTrainingRuns(runnerId, season, type)
        val workoutResultsFuture = runnerProfileAsyncHelper.getWorkoutResults(runnerId, season, type)
        val goalsFuture = runnerProfileAsyncHelper.getGoalForRunner(runnerId, season, type)
        val meetResultsFuture = runnerProfileAsyncHelper.getMeetResults(runnerId, season, SortingMethodContainer.RECENT_DATE, 20)
        val trackResultsFuture = runnerProfileAsyncHelper.getTrackMeetPerformances(runnerId, season)
        val trainingRunSummaryFuture = runnerProfileAsyncHelper.getTrainingRunSummary(runnerId, season, includeWarmUps, type)
        val trackPRsFuture = runnerProfileAsyncHelper.getTrackPRs(runnerId)
        val trackSBsFuture = runnerProfileAsyncHelper.getTrackSBs(runnerId, season)
        val achievementsFuture = runnerProfileAsyncHelper.getAchievements(runnerId = runner.id)
        val crossTrainingFuture = runnerProfileAsyncHelper.getCrossTrainingWorkouts(runnerId, season, type)
        val seasonTrainingCountFuture = runnerProfileAsyncHelper.getSummerTrainingAwardStatus(runnerId, type, season, team)



        //////// end async operations /////////


        // build response

        val prRank = prRanksFuture.get().firstOrNull { it.runner.id == runnerId }
        val sbRank = sbRanksFuture.get().firstOrNull { it.runner.id == runnerId }
        val consistencyRank = meetSplitConsistencyRanksFuture.get().firstOrNull { it.runner.id == runnerId }
        val timeTrailProgressionRank = timeTrialProgressionRanksFuture.get().firstOrNull { it.runner.id == runnerId }
        val trainingDistanceRank = distanceRunRanksFuture.get().firstOrNull { it.runner.id == runnerId }
        val trainingRuns = trainingRunsFuture.get().trainingRunResults.sortedByDescending { it.trainingRun.date }
        val workoutResults = workoutResultsFuture.get()
        val goals = goalsFuture.get()
        val meetResults = meetResultsFuture.get().map { it.performance }.flatten()
        val trackResults = trackResultsFuture.get()
        val trainingRunSummary = trainingRunSummaryFuture.get()
        val trackPRs = trackPRsFuture.get()
        val trackSBs = trackSBsFuture.get()
        val achievements = achievementsFuture.get()
        val crossTrainingRecords = crossTrainingFuture.get()
        val summerTrainingCount = seasonTrainingCountFuture.get()



        return RunnerProfileDTOV2(runner, prRank, sbRank, consistencyRank, trainingDistanceRank, timeTrailProgressionRank,
                goals.goals, trainingRuns, workoutResults, meetResults.sortedBy { it.meetDate }, trackResults.sortedBy { it.meet.date }, trainingRunSummary,
                achievements, trackPRs, trackSBs, crossTrainingRecords, summerTrainingCount
        )


    }

    fun buildRunnerProfile(name: String): RunnerProfileDTO {

        val runner =
                try {
                    runnerRepository.findByNameContaining(name).first()
                } catch (e: Exception) {
                    throw RunnerNotFoundByPartialNameException("No runner matching the given name: $name")
                }

        val startDate = Date.valueOf(MeetPerformanceController.CURRENT_YEAR + "-01-01")
        val endDate = Date.valueOf(MeetPerformanceController.CURRENT_YEAR + "-12-31")

        val seasonBests = seasonBestService.getSeasonBestsByName(name, listOf(startDate to endDate), false)
        var seasonBest: RunnerMeetSplitDTO? = null
        if (seasonBests.isNotEmpty()) {
            if (seasonBests.first().seasonBest.isNotEmpty()) {
                seasonBest = RunnerMeetSplitDTO(seasonBests.first().seasonBest.first(), null)
            }
        }

        ////////// async oeprations //////////
        val meetSplitsAsync = runnerProfileAsyncHelper.getAllMeetSplits(name, startDate, endDate)
        val workoutConsistency = runnerProfileAsyncHelper.getWorkoutConsistency(startDate, endDate)
        val raceConsistency = runnerProfileAsyncHelper.getRaceConsistency(startDate, endDate)
        val totalConsistency =  runnerProfileAsyncHelper.getTotalConsistency(startDate, endDate, 1.0)
        val progressions = runnerProfileAsyncHelper.getTimeTrialProgression(startDate, endDate)
        val lastYearStart = Date.valueOf((MeetPerformanceController.CURRENT_YEAR.toInt() - 1).toString() + "-01-01")
        val lastYearEnd = Date.valueOf((MeetPerformanceController.CURRENT_YEAR.toInt() - 1).toString() + "-12-31")
        val lastYearPerformances =
                runnerProfileAsyncHelper.getMeetPerformancesLastYear(name, lastYearStart, lastYearEnd, SortingMethodContainer.RECENT_DATE, 20)
        val allSeasonBestsAsync = runnerProfileAsyncHelper.getAllSeasonBests(startDate, endDate)
        val timeTrials = runnerProfileAsyncHelper.getTimeTrials(startDate, endDate)
        val goalAsync = runnerProfileAsyncHelper.getGoalForRunner(name, MeetPerformanceController.CURRENT_YEAR)

        //////// end async operations /////////

        val mostConsistentRace = meetSplitsAsync.get().sortedBy { it.meetSplitsDTO!!.toMeetSplit().calculateSpread() }
                .firstOrNull()

        val seasonBestSplits = getSplitsForMeetPerformances(seasonBest?.meetPerformanceDTO, runner)

        val prs = personalRecordService.getPRsByName(name, false)
        var pr: MeetPerformanceDTO? = null

        if (prs.isNotEmpty()) {
            if (prs.first().pr.isNotEmpty())
                pr = prs.first().pr.first()
        }

        var prSplits: RunnerMeetSplitDTO? = null

        if (pr !=null ) {
            prSplits = getSplitsForMeetPerformances(pr, runner)
        }

        // todo
        //val goals: List<String?> = goalAsync.get().first().times

        val goals: List<String?> = emptyList()

        val workouts = workoutResultService.getWorkoutsForRunner(startDate, endDate, name, 0, "", "").workouts
        var lastWorkout: RunnerWorkoutResultsDTO? = null
        if (workouts.isNotEmpty()) {
            lastWorkout = workouts.sortedBy { it.workout.date }.first()
        }

        val workoutRank = workoutConsistency.get()!!
                .filter { it.runner.id == runner.id }
                .map {
                    ValuedRank("Workout Rank", it.consistencyRank.rank, it.consistencyRank.consistencyValue.toMinuteSecondString())
                }
                .firstOrNull()

        val raceRank = raceConsistency.get()!!
                .filter { it.runner.id == runner.id }
                .map {
                    ValuedRank("Race Rank", it.consistencyRank.rank, it.consistencyRank.consistencyValue.toMinuteSecondString())
                }
                .firstOrNull()


        val combinedRank = totalConsistency.get()!!
                .filter { it.runner.id == runner.id }
                .map { ValuedRank("Workout And Race Consistency Rank", it.consistencyRank.rank, it.consistencyRank.consistencyValue.toMinuteSecondString()) }
                .firstOrNull()


        val timeTrialTime: String? = progressions.get()!!
                .filter { it.runner.id == runner.id }
                .firstOrNull()
                ?.adjustedTimeTrial

        val progressionRank = progressions.get()!!
                .filter { it.runner.id == runner.id }
                .map { ValuedRank( "Progression Rank", it.rank, it.improvement ) }
                .firstOrNull()


        ////////// build season best rank //////////

        val allSeasonBests = allSeasonBestsAsync.get()!!.map { it.runner to it.seasonBest }
                .filter {
                    it.second.isNotEmpty()
                }
                .map {
                    it.first.id to it.second.first().time
                }.toMutableList()

        // get time trial results as fall backs for runners with no season best yet

        val allAdjustedTimeTrials = timeTrials.get()!!
                .map { it.runnerId to it.time }
                .filter {
                    !allSeasonBests.map { sb -> sb.first }.contains(it.first)
                }

        allSeasonBests.addAll(allAdjustedTimeTrials)

        val timeRank = allSeasonBests.mapIndexed { index, it ->
            Triple(index + 1, it.first, it.second)
        }.sortedBy { it.third }.filter { it.second == runner.id }
                .map { NamedRank("Season Best", it.first) }.firstOrNull()


        /////// meet info for runner from last year //////

        val upcomingMeets = meetRepository.findByDateBetween(startDate, endDate).filter {
            it.date.after(Date.from(Instant.now()))
        }.sortedByDescending { it.date }

        var upcomingMeet: Meet? = null
        if (upcomingMeets.isNotEmpty()) {
            upcomingMeet = upcomingMeets.first()
        }

        val lastYearPerformanceAtUpcomingMeet: MeetPerformanceDTO?
        var upComingMeetSplitsLastYear: RunnerMeetSplitDTO? = null

        if (upcomingMeet != null) {
            val performances = lastYearPerformances.get()!!
                    .map {
                        it.performance
                    }.flatten()

            lastYearPerformanceAtUpcomingMeet = performances.firstOrNull {
                it.meetName.equals(upcomingMeet.name, ignoreCase = true)
            }

            upComingMeetSplitsLastYear = getSplitsForMeetPerformances(lastYearPerformanceAtUpcomingMeet, runner)
        }

        return RunnerProfileDTO(runner, timeTrialTime, goals, seasonBestSplits, prSplits, mostConsistentRace, lastWorkout,
                workoutRank, raceRank, combinedRank, progressionRank, timeRank, upComingMeetSplitsLastYear)
    }

    fun getSplitsForMeetPerformances(meetPerformance: MeetPerformanceDTO?, runner: Runner): RunnerMeetSplitDTO? {

        var splits: RunnerMeetSplitDTO? = null

        if (meetPerformance != null) {

            val startDate = TimeUtilities.getFirstDayOfYear()
            val endDate = TimeUtilities.getLastDayOfYear()

            val meetSplits = meetMileSplitService.getAllMeetMileSplitsForRunner(runner.name, startDate, endDate).mileSplits
            splits = meetSplits.firstOrNull { it.meetPerformanceDTO.meetName == meetPerformance.meetName }

            if (splits == null) {
                splits = RunnerMeetSplitDTO(meetPerformance, null)
            }
        }

        return splits
    }

    fun getRunnerWordCount(runnerId: Int, season: String): Int {

        var meetLogsWordCount = meetLogRepository.findByRunnerId(runnerId)
                .map {
                    var coachNotesWords = if (it.coachNotes != null) {
                        it.coachNotes!!.split(" ").size
                    } else {
                        0
                    }

                    var runnerNotes = if (it.notes != null) {
                        it.notes!!.split(" ").size
                    } else {
                        0
                    }

                    return@map coachNotesWords + runnerNotes

                }.sum()

        var trainingRunNotesCount  = runnersTrainingRunRepository.findByRunnerId(runnerId)
                .map {
                    var coachNotesWords = if (it.coachNotes != null) {
                        it.coachNotes!!.split(" ").size
                    } else {
                        0
                    }

                    var runnerNotes = if (it.notes != null) {
                        it.notes!!.split(" ").size
                    } else {
                        0
                    }

                    return@map coachNotesWords + runnerNotes
                }.sum()

        var workoutNotesCount = runnerWorkoutDistanceRepository.findByRunnerId(runnerId)
                .map {
                    var coachNotesWords = if (it.coachNotes != null) {
                        it.coachNotes!!.split(" ").size
                    } else {
                        0
                    }

                    var runnerNotes = if (it.notes != null) {
                        it.notes!!.split(" ").size
                    } else {
                        0
                    }

                    return@map coachNotesWords + runnerNotes
                }
                .sum()

        return workoutNotesCount + trainingRunNotesCount + meetLogsWordCount

    }
}