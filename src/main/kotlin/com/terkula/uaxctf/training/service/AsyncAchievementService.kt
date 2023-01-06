package com.terkula.uaxctf.training.service

import com.terkula.uaxctf.statisitcs.model.toMeetPerformanceDTO
import com.terkula.uaxctf.statistics.dto.MeetPerformanceDTO
import com.terkula.uaxctf.statistics.dto.streak.StreakDTO
import com.terkula.uaxctf.statistics.repository.MeetRepository
import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.statistics.response.RunnerMeetSplitResponse
import com.terkula.uaxctf.statistics.service.MeetMileSplitService
import com.terkula.uaxctf.statistics.service.MeetPerformanceService
import com.terkula.uaxctf.statistics.service.PersonalRecordService
import com.terkula.uaxctf.util.calculateSecondsFrom
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.AsyncResult
import org.springframework.stereotype.Service
import java.util.concurrent.Future

@Service
class AsyncAchievementService(
    val meetRepository: MeetRepository,
    val runnerRepository: RunnerRepository,
    val trainingRunsService: TrainingRunsService,
    val meetPerformanceService: MeetPerformanceService,
    val meetSplitService: MeetMileSplitService,
    val prService: PersonalRecordService
) {

    @Async
    open fun getRunnersTotalTrainingDistance(runnerId: Int): Future<Pair<Double, Int>> {
        return AsyncResult(trainingRunsService.getAllTrainingMilesRunForARunnerCareer(runnerId))
    }

    @Async
    open fun wonRaces(runnerId: Int): Future<List<MeetPerformanceDTO>> {

        val meets = meetRepository.findAll().associateBy { it.id }

        return AsyncResult(meetPerformanceService.getFirstPlacePerformancesForRunner(runnerId).toMeetPerformanceDTO(meets))
    }

    @Async
   open fun getTotalPassesLastMile(runnerId: Int): Future<Int> {
        return AsyncResult(meetPerformanceService.getTotalPassesLastMileForRunner(runnerId))
    }

    @Async
    fun getConsistentRaces(runnerId: Int, targetSpread: Int): Future<RunnerMeetSplitResponse> {

        val splits = meetSplitService.getMeetSplitsForRunner(runnerId)

        val response = RunnerMeetSplitResponse(splits.runner,
                splits
                .mileSplits
                .filter {
                    it.meetSplitsDTO!!.isConsistentRace()
                }
            )

        return AsyncResult(response)
        }

    // ideally, return all races where the first mile split was faster than targetPercent % of PR pace at the time of the meet

//    @Async
//    fun getFastStarts(runnerId: Int, targetPercent): RunnerMeetSplitResponse {
//
//
//
//    }

    @Async fun getTotalPrsSet(runnerId: Int): Future<List<MeetPerformanceDTO>> {
        return AsyncResult(prService.getPRsForRunner(runnerId))
    }

    @Async fun getSkullsEarnedStreak(runnerId: Int): Future<StreakDTO> {
        return AsyncResult(meetPerformanceService.getSkullsEarnedStreak(runnerId))
    }

    @Async fun getSkullsStreakAchievement(runnerId: Int): Future<StreakDTO> {
        return AsyncResult(meetPerformanceService.getSkullsEarnedStreak(runnerId))
    }

    @Async fun getSkullsEarnedTotal(runnerId: Int): Future<Int> {
        return AsyncResult(meetPerformanceService.getSkullsEarnedTotal(runnerId))
    }
}