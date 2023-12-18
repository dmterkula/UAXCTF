package com.terkula.uaxctf.statistics.service.track

import com.terkula.uaxctf.statistics.dto.RunnerGoalDTO
import com.terkula.uaxctf.statistics.dto.RunnersMetGoals
import com.terkula.uaxctf.statistics.dto.track.TrackMetGoalDTO
import com.terkula.uaxctf.statistics.dto.track.TrackPerformancesDTO
import com.terkula.uaxctf.statistics.repository.track.TrackMeetRepository
import com.terkula.uaxctf.statistics.response.MetGoalResponse
import com.terkula.uaxctf.statistics.response.track.RunnerTrackMetGoalDTO
import com.terkula.uaxctf.statistics.response.track.TrackMetGoalsResponse
import com.terkula.uaxctf.statistics.service.XcGoalService
import com.terkula.uaxctf.util.calculateSecondsFrom
import com.terkula.uaxctf.util.getYearString

import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.AsyncResult
import org.springframework.stereotype.Service
import java.util.concurrent.Future

@Service
open class TrackMeetSummaryAsyncHelper(
     private val trackMeetRepository: TrackMeetRepository,
     private val seasonBestService: TrackSBService,
     private val personalRecordService: TrackPRService,
     private val goalService: XcGoalService
 ) {

    @Async
    open fun getSeasonBestsAtMeet(meetUUID: String, includeSplits: Boolean): Future<List<TrackPerformancesDTO>> {

        val meet = trackMeetRepository.findByUuid(meetUUID)

        return AsyncResult(seasonBestService.getAllSBsAsOfDate(meet.get().date, includeSplits, "", meet.get().date.getYearString())
                .filter { it.bestResults.firstOrNull() != null }
                .filter { it.bestResults.isNotEmpty() }
                .filter { it.bestResults.first().meet.uuid == meetUUID }
        )
    }

    @Async
    open fun getPRsAtMeet(meetUUID: String, includeSplits: Boolean): Future<List<TrackPerformancesDTO>> {

        val meet = trackMeetRepository.findByUuid(meetUUID)

       return AsyncResult(personalRecordService.getAllPRsAsOfDate(meet.get().date, meet.get().date.getYearString(), includeSplits, meet.get().date.getYearString())
                .filter { it.bestResults.firstOrNull() != null }
                .filter { it.bestResults.isNotEmpty() }
                .filter { it.bestResults.first().meet.uuid == meetUUID }
        )
    }

    fun getGoalsMetAtMeet(meetUUID: String, sbsAtMeet: List<TrackPerformancesDTO>): TrackMetGoalsResponse {

        val meet = trackMeetRepository.findByUuid(meetUUID).get()

        val runnerToSb = sbsAtMeet.map { it.runner to it }

        val runnerIdToGoals = goalService.getGoalsForSeason(meet.date.getYearString(), xcOnly = false, trackOnly = true)
                .map {
                    it.runner.id to it.goals
                }
                .toMap()



        // foreach SB
        // find the runner goals for that event, keeping only the one that are greaterThanEqual to SB time,
        // then only keep the best goal met.



       val metGoals: List<List<RunnerTrackMetGoalDTO>> = runnerToSb.map {
             val runnersMetGoalsForEachEventTheySBIn: List<RunnerTrackMetGoalDTO> = it.second.bestResults.map { sb ->
                 val metGoalForEvent: TrackMetGoalDTO? = if (runnerIdToGoals[it.first.id] == null) {
                     null
                } else {
                    val runnerMetEventGoal = runnerIdToGoals[it.first.id]!!
                            .filter { goal -> goal.type.equals("time", ignoreCase = true) }
                            .filter { goal -> goal.event!! == sb.event }
                            .filter { goal -> goal.value.calculateSecondsFrom() >= sb.best.time.calculateSecondsFrom()  }
                            .sortedBy { goal -> goal.value.calculateSecondsFrom() }
                            .firstOrNull()

                     if (runnerMetEventGoal == null) {
                         null
                     } else {
                         TrackMetGoalDTO(runnerMetEventGoal.event!!, runnerMetEventGoal.value, meet, sb.best)
                     }
                }

                 if (metGoalForEvent == null) {
                     return@map null
                 } else {
                     return@map it.first to metGoalForEvent!!
                 }
            }.filterNotNull()
                     .groupBy { it.first }
                     .map { runnerToMetGoals -> RunnerTrackMetGoalDTO(runnerToMetGoals.key, runnerToMetGoals.value.map { metGoals-> metGoals.second }) }

           return@map runnersMetGoalsForEachEventTheySBIn

        }

        return TrackMetGoalsResponse(metGoals.flatten().groupBy { it.runner }.map { RunnerTrackMetGoalDTO(it.key, it.value.map { it.metGoals }.flatten()) })

    }

}


