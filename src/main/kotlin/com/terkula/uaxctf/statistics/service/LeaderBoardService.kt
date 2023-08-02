package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statistics.dto.*
import com.terkula.uaxctf.statistics.dto.leaderboard.RankedAchievementDTO
import com.terkula.uaxctf.statistics.dto.leaderboard.RankedMeetResultDTO
import com.terkula.uaxctf.statistics.dto.leaderboard.RankedSeasonBestDTO
import com.terkula.uaxctf.statistics.dto.leaderboard.RankedTryoutDTO
import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.statistics.request.SortingMethodContainer
import com.terkula.uaxctf.training.response.RankedRunnerDistanceRunDTO
import com.terkula.uaxctf.training.service.TrainingRunsService
import com.terkula.uaxctf.util.TimeUtilities
import com.terkula.uaxctf.util.calculateSecondsFrom
import com.terkula.uaxctf.util.round
import org.springframework.stereotype.Service

@Service
class LeaderBoardService(
    val prService: PersonalRecordService,
    val seasonBestService: SeasonBestService,
    val consistencyRankService: ConsistencyRankService,
    val trainingRunsService: TrainingRunsService,
    val timeTrialService: TimeTrialService,
    val meetPerformanceService: MeetPerformanceService,
    val runnerRepository: RunnerRepository,
    val meetMileSplitService: MeetMileSplitService
) {

    fun getPRLeaderBoard(pageSize: Int): List<RankedPRDTO> {

        val allPRs: List<PRDTO> = prService.getAllPRs("2017", filterClass = "", adjustForDistance = false, sortingMethodContainer = SortingMethodContainer.TIME)
                .filter { it.pr.isNotEmpty() }
                .take(pageSize)

        return allPRs.mapIndexed { index, it ->
            RankedPRDTO(it.runner, it.pr.first(), index + 1)
        }

    }

    fun getTryoutLeaderBoard(pageSize: Int, adjustTo5k: Boolean): List<RankedTryoutDTO> {

        val allTimeTrials = timeTrialService.getAllTimeTrialResults(adjustTo5k)
                .groupBy { it.runner }
                .map {
                    it.key to it.value.sortedBy { it.time.calculateSecondsFrom() }.firstOrNull()
                }
                .filter { it.second != null }
                .map { it.first to it.second!!}
                .sortedBy { it.second.time.calculateSecondsFrom() }
                .mapIndexed { index, it ->
                    RankedTryoutDTO(it.first, index + 1, it.second)
                }

        return allTimeTrials

    }

    fun getSeasonBestLeaderBoard(season: String): List<RankedSeasonBestDTO> {
        val currentSBs = seasonBestService.getAllSeasonBests(TimeUtilities.getFirstDayOfGivenYear(season), TimeUtilities.getLastDayOfGivenYear(season), false)

       return currentSBs.filter {
            it.seasonBest.isNotEmpty()
        }.mapIndexed() { index, it ->
            RankedSeasonBestDTO(it.runner, it.seasonBest.first(), index + 1)
        }

    }

    fun getMeetTimeLeaderBoard(meet: String, count: Int): List<RankedMeetResultDTO> {
        val topPerformancesAtMeet = meetPerformanceService.getMeetPerformancesAtMeetName(meet, TimeUtilities.getFirstDayOfGivenYear("2017"), TimeUtilities.getLastDayOfYear(), SortingMethodContainer.TIME, count, false)
                .filter { it.performance.firstOrNull() !=  null }

        return topPerformancesAtMeet.mapIndexed{ index, it ->
            RankedMeetResultDTO(it.runner, it.performance.first(), index + 1)
        }

    }

   fun getRaceConsistentRankLeaderBoard(season: String): List<RankedRunnerConsistencyDTO> {

       return consistencyRankService.getRunnersOrderedByMostConsistentRaces(TimeUtilities.getFirstDayOfGivenYear(season), TimeUtilities.getLastDayOfGivenYear(season))
               .map {
                   RankedRunnerConsistencyDTO(it.runner, it.consistencyRank.consistencyValue.round(2), it.consistencyRank.rank)
               }
   }

    fun getRaceConsistentRankAchievementLeaderBoardSeason(season: String): List<RankedAchievementDTO> {

        return meetMileSplitService.getRankedConsistentMeetSplitsAchievement(season)
    }

    fun getRaceConsistentRankAchievementLeaderBoardCareer(): List<RankedAchievementDTO> {

        return meetMileSplitService.getMeetSplitsForRunnersCareer()
                .map { it.key to it.value }
                .map { it.first to it.second.filter { split -> split.isConsistentRace() }}
                .map {
                    it.first to it.second.count()
                }
                .sortedByDescending { it.second }
                .mapIndexed {index, it ->
                        RankedAchievementDTO(it.first, index + 1, it.second)
                }
    }

    fun getDistanceRunRankLeaderBoard(season: String): List<RankedRunnerDistanceRunDTO> {

        // need to get workout splits sum and total meets run

        return trainingRunsService.getAllTrainingMilesRunByRunner(season)

    }

    fun getDistanceRunRankCareerLeaderBoard(): List<RankedRunnerDistanceRunDTO> {

        // need to get workout splits sum and total meets run

        return trainingRunsService.getAllTrainingMilesRunByRunnerForCareer()

    }

    fun getRunLoggedCountRankLeaderBoardCareer(): List<RankedAchievementDTO> {


        return trainingRunsService.getRankedRunnersTrainingRunCountForCareer()

    }

    fun getRunLoggedCountRankLeaderBoardSeason(season: String): List<RankedAchievementDTO> {


        return trainingRunsService.getRankedRunnersTrainingRunCountForSeason(season)

    }

    fun getTimeTrialProgressionLeaderBoard(season: String): List<TimeTrialImprovementDTO> {
        return timeTrialService.getRankedProgressionSinceTimeTrial(TimeUtilities.getFirstDayOfGivenYear(season), TimeUtilities.getLastDayOfGivenYear(season), false)
    }

    fun getSkullsTotalLeaderboard(): List<RankedAchievementDTO> {

        val runners = runnerRepository.findAll()

        val races = meetPerformanceService.getAllRaces()

        return races.groupBy { it.runner }
                .map {
                    it.key to it.value.map { runnerPerf -> runnerPerf.performance.first()}.sumOf { perf-> perf.skullsEarned }
                }
                .sortedByDescending { it.second }
                .mapIndexed { index, it ->
                    RankedAchievementDTO(it.first, index + 1, it.second)
                }
    }

    fun getSkullsTotalSeasonLeaderboard(season: String): List<RankedAchievementDTO> {

        val runners = runnerRepository.findAll()

        val races = meetPerformanceService.getRacesInSeason(season)

        return races.groupBy { it.runner }
                .map {
                    it.key to it.value.map { runnerPerf -> runnerPerf.performance.first()}.sumOf { perf-> perf.skullsEarned }
                }
                .sortedByDescending { it.second }
                .mapIndexed { index, it ->
                    RankedAchievementDTO(it.first, index + 1, it.second)
                }
    }

    fun getSkullsStreakSeasonLeaderboard(season: String, active: Boolean?): List<RankedAchievementDTO> {

        val runners = runnerRepository.findAll().map { it.id to it }.toMap()

        var activeValue = true
        if (active == null || !active) {
            activeValue = false
        }

        return meetPerformanceService.getRacesInSeason(season)
                .groupBy { it.runner }
                .map { it.key to it.value.map { perf -> perf.performance.first() } }
                .map { it.first to it.second.sortedBy { perf -> perf.meetDate } }
                .map {
                    it.first to meetPerformanceService.getSkullStreakForRaces(it.first, it.second, activeValue)
                }
                .sortedByDescending { it.second.currentStreak }
                .toMutableList()
                .mapIndexed { index, it ->
                    RankedAchievementDTO(it.first, index + 1, it.second.currentStreak)
                }
    }

    fun getSkullsStreakCareerLeaderboard(active: Boolean?): List<RankedAchievementDTO> {

        var activeValue = true
        if (active == null || !active) {
            activeValue = false
        }

        return meetPerformanceService.getAllRaces()
                .groupBy { it.runner }
                .map { it.key to it.value.map { perf -> perf.performance.first() } }
                .map { it.first to it.second.sortedBy { perf -> perf.meetDate } }
                .map {
                    it.first to meetPerformanceService.getSkullStreakForRaces(it.first, it.second, activeValue)
                }
                .sortedByDescending { it.second.currentStreak }
                .toMutableList()
                .mapIndexed { index, it ->
                    RankedAchievementDTO(it.first, index + 1, it.second.currentStreak)
                }
    }

    fun getPassesLastMileLeaderboardSeason(season: String): List<RankedAchievementDTO> {

        return meetPerformanceService.getRacesInSeason(season)
                .groupBy { it.runner }
                .map { it.key to it.value.map { perf -> perf.performance.first() } }
                .map { it.first to it.second.sumOf { perf-> perf.passesLastMile } }
                .sortedByDescending { it.second }
                .mapIndexed{ index, it ->
                    RankedAchievementDTO(it.first, index + 1, it.second)
                }

    }

    fun getPassesLastMileLeaderboardCareer(): List<RankedAchievementDTO> {

       return  meetPerformanceService.getAllRaces()
                .groupBy { it.runner }
                .map { it.key to it.value.map { perf -> perf.performance.first() } }
                .map { it.first to it.second.sumOf { perf-> perf.passesLastMile } }
                .sortedByDescending { it.second }
                .mapIndexed{ index, it ->
                    RankedAchievementDTO(it.first, index + 1, it.second)
                }
    }

}