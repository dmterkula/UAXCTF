package com.terkula.uaxctf.statistics.service.track

import com.fasterxml.jackson.databind.ObjectMapper
import com.terkula.uaxctf.statisitcs.model.track.*
import com.terkula.uaxctf.statistics.repository.*
import com.terkula.uaxctf.statistics.repository.track.TrackMeetPerformanceRepository
import com.terkula.uaxctf.statistics.repository.track.TrackMeetRepository
import com.terkula.uaxctf.statistics.request.track.CreateTrackMeetResultRequest
import com.terkula.uaxctf.statistics.response.track.TrackMeetPerformanceResponse
import com.terkula.uaxctf.statistics.response.track.TrackMeetSummaryResponse
import com.terkula.uaxctf.util.TimeUtilities
import com.terkula.uaxctf.util.getYearString
import com.terkula.uaxctf.util.subtractDays
import org.springframework.stereotype.Component
import java.sql.Time

@Component
class TrackMeetPerformanceService(
    private val meetRepository: TrackMeetRepository,
    private val trackMeetPerformanceRepository: TrackMeetPerformanceRepository,
    private val runnerRepository: RunnerRepository,
    private val trackMeetSummaryAsyncHelper: TrackMeetSummaryAsyncHelper
) {
    fun getTrackMeetResults(meetUUID: String): List<TrackMeetPerformanceDTO> {

        val meet = meetRepository.findByUuid(meetUUID)
        if (!meet.isPresent) {
            return emptyList()
        } else {

            val runners = runnerRepository.findByGraduatingClassGreaterThanEqual((meet.get().date.getYearString())).map { it.id to it }.toMap()
            return trackMeetPerformanceRepository.findByMeetId(meetUUID)
                    .groupBy { it.runnerId }
                   .map { TrackMeetPerformanceDTO(
                           meet.get(), runners[it.key]!!, it.value.toTrackMeetPerformancesResponses(meet.get().name, meet.get().date)
                   ) }
        }
    }

    fun getTrackMeetResultsForRunner(runnerId: Int, season: String): List<TrackMeetPerformanceDTO> {

        val startDate = TimeUtilities.getFirstDayOfGivenYear(season).subtractDays(90)
        val endDate = TimeUtilities.getLastDayOfGivenYear(season).subtractDays(150)

        val meets = meetRepository.findByDateBetween(startDate, endDate)
        val runner = runnerRepository.findById(runnerId).get()

         return meets.map {
             TrackMeetPerformanceDTO(it, runner,
                     trackMeetPerformanceRepository.findByMeetIdAndRunnerId(it.uuid, runnerId).map { result ->
                         result.toTrackMeetPerformanceResponse(it.name, it.date)
                     }
             )
        }.filter { it.results.isNotEmpty() }

    }

    fun getTrackMeetResults(meetName: String, season: String): List<TrackMeetPerformanceDTO> {

        val startDate = TimeUtilities.getFirstDayOfGivenYear(season)
        val endDate = TimeUtilities.getLastDayOfGivenYear(season)

        val meet = meetRepository.findByNameContainingAndDateBetween(meetName, startDate, endDate)
        if (meet.isEmpty()) {
            return emptyList()
        } else {

            val runners = runnerRepository.findByGraduatingClassGreaterThanEqual(meet.first().date.getYearString()).map { it.id to it }.toMap()
            return trackMeetPerformanceRepository.findByMeetId(meet.first().uuid)
                    .groupBy { it.runnerId }
                    .map { TrackMeetPerformanceDTO(
                            meet.first(), runners[it.key]!!, it.value.toTrackMeetPerformancesResponses(meet.first().name, meet.first().date)
                    ) }
        }
    }

    fun createTrackMeetResult(createTrackMeetResultRequest: CreateTrackMeetResultRequest): TrackMeetPerformanceDTO? {

        val objectMapper = ObjectMapper()
        val startDate = TimeUtilities.getFirstDayOfGivenYear(createTrackMeetResultRequest.season)
        val endDate = TimeUtilities.getLastDayOfGivenYear(createTrackMeetResultRequest.season)

        val meets = if (createTrackMeetResultRequest.meetId.isNotEmpty()) {
            val result = meetRepository.findByUuid(createTrackMeetResultRequest.meetId)
            if (result.isPresent) {
                listOf(result.get())
            } else {
                emptyList()
            }
        } else {
            meetRepository.findByNameContainingAndDateBetween(createTrackMeetResultRequest.meetName, startDate, endDate)
        }

        if (meets.isEmpty()) {
            return null
        }

        val runner = runnerRepository.findById(createTrackMeetResultRequest.runnerId).get()

        val meet = meets.first()

        val performances = trackMeetPerformanceRepository.findByMeetIdAndRunnerId(meet.uuid, runner.id)

        if (performances.isEmpty()) {

           val createdResults: MutableList<TrackMeetPerformance> = mutableListOf()
            createTrackMeetResultRequest.results.forEach {
                val splitsString = objectMapper.writeValueAsString(it.splits)
                val newPerformance = TrackMeetPerformance(meet.uuid, it.uuid, runner.id, it.time, it.place, it.event, it.isSplit, splitsString)
                trackMeetPerformanceRepository.save(newPerformance)
                createdResults.add(newPerformance)
            }

            return TrackMeetPerformanceDTO(meet, runner, createdResults.toTrackMeetPerformancesResponses(meet.name, meet.date))

        } else {
            val createdResults: MutableList<TrackMeetPerformance> = mutableListOf()
            createTrackMeetResultRequest.results.map {
                performances.firstOrNull { perf -> perf.event == it.event } to it
            }.forEach {
                var splitsString = objectMapper.writeValueAsString(it.second.splits)
                if (it.first != null) {
                    it.first!!.place = it.second.place
                    it.first!!.time = it.second.time
                    it.first!!.event = it.second.event
                    it.first!!.isSplit = it.second.isSplit
                    it.first!!.splits = splitsString
                    trackMeetPerformanceRepository.save(it.first!!)
                    createdResults.add(it.first!!)
                } else {
                    // existing performance doesn't exist for this event, create it
                    val newResult = TrackMeetPerformance(meet.uuid, it.second.uuid, runner.id, it.second.time,
                            it.second.place, it.second.event, it.second.isSplit, splitsString)
                    trackMeetPerformanceRepository.save(newResult)
                    createdResults.add(newResult)
                }
            }

            return TrackMeetPerformanceDTO(meet, runner, createdResults.toTrackMeetPerformancesResponses(meet.name, meet.date))

        }

    }

    fun getTrackMeetSummaryForMeetNameAndSeason(meetName: String, season: String, includeSplits: Boolean): TrackMeetSummaryResponse {

        val meet = meetRepository.findByNameContainingAndDateBetween(meetName, TimeUtilities.getFirstDayOfGivenYear(season), TimeUtilities.getLastDayOfGivenYear(season))

        return getTrackMeetSummary(meet.first().uuid, includeSplits)

    }

    fun getTrackMeetSummary(meetUUID: String, includeSplits: Boolean): TrackMeetSummaryResponse {

        val seasonBest = trackMeetSummaryAsyncHelper.getSeasonBestsAtMeet(meetUUID, includeSplits)
        val prs = trackMeetSummaryAsyncHelper.getPRsAtMeet(meetUUID, includeSplits)


        ////// END ASYNC OPS //////////

        val blockedSeasonBests = seasonBest.get()
        val blockedPrs = prs.get()

        val metGoals = trackMeetSummaryAsyncHelper.getGoalsMetAtMeet(meetUUID, blockedSeasonBests)


        return TrackMeetSummaryResponse(blockedPrs, blockedSeasonBests, metGoals)

    }
}

