package com.terkula.uaxctf.statistics.service.track

import com.fasterxml.jackson.databind.ObjectMapper
import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.statisitcs.model.track.*
import com.terkula.uaxctf.statistics.controller.firebase.FirebaseMessageService
import com.terkula.uaxctf.statistics.dto.track.TrackPerformancesDTO
import com.terkula.uaxctf.statistics.dto.track.TrackTopResult
import com.terkula.uaxctf.statistics.repository.*
import com.terkula.uaxctf.statistics.repository.track.TrackMeetPerformanceRepository
import com.terkula.uaxctf.statistics.repository.track.TrackMeetRepository
import com.terkula.uaxctf.statistics.request.track.CreateTrackMeetResultRequest
import com.terkula.uaxctf.statistics.request.track.TrackResult
import com.terkula.uaxctf.statistics.response.track.TrackMeetSummaryResponse
import com.terkula.uaxctf.util.TimeUtilities
import com.terkula.uaxctf.util.calculateSecondsFrom
import com.terkula.uaxctf.util.getYearString
import com.terkula.uaxctf.util.subtractDays
import org.springframework.stereotype.Component

@Component
class TrackMeetPerformanceService(
    private val meetRepository: TrackMeetRepository,
    private val trackMeetPerformanceRepository: TrackMeetPerformanceRepository,
    private val runnerRepository: RunnerRepository,
    private val trackMeetSummaryAsyncHelper: TrackMeetSummaryAsyncHelper,
    private val firebaseMessageService: FirebaseMessageService,
    private val trackPRService: TrackPRService,
    private val trackSBService: TrackSBService,
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

        val PRs = trackPRService.getARunnersPRs(runner.id, false, "", false)
        val SBs = trackSBService.getARunnersSBs(runner.id, false, "", startDate.getYearString(), false)

        val meet = meets.first()

        val performances = trackMeetPerformanceRepository.findByMeetIdAndRunnerId(meet.uuid, runner.id)

        if (performances.isEmpty()) {

           val createdResults: MutableList<TrackMeetPerformance> = mutableListOf()
            createTrackMeetResultRequest.results.forEach {
                val splitsString = objectMapper.writeValueAsString(it.splits)
                val newPerformance = TrackMeetPerformance(meet.uuid, it.uuid, runner.id, it.time, it.place, it.event, it.isSplit, splitsString)
                trackMeetPerformanceRepository.save(newPerformance)
                createdResults.add(newPerformance)
                var sentPRNotification = sendPRNotificationIfPR(runner, meet, PRs, it)
                if (!sentPRNotification) {
                    sendSBNotificationIfSB(runner, meet, PRs, it)
                }
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
                    // this event record already existed, don't send notifications.
                } else {
                    // existing performance doesn't exist for this event, create it
                    val newResult = TrackMeetPerformance(meet.uuid, it.second.uuid, runner.id, it.second.time,
                            it.second.place, it.second.event, it.second.isSplit, splitsString)
                    trackMeetPerformanceRepository.save(newResult)
                    createdResults.add(newResult)
                    val sentPRNotification = sendPRNotificationIfPR(runner, meet, PRs, it.second)
                    if (!sentPRNotification) {
                        sendSBNotificationIfSB(runner, meet, SBs, it.second)
                    }
                }
            }

            return TrackMeetPerformanceDTO(meet, runner, createdResults.toTrackMeetPerformancesResponses(meet.name, meet.date))

        }

    }

    fun sendPRNotificationIfPR(runner: Runner, meet: TrackMeet, PRs: TrackPerformancesDTO, trackResult: TrackResult): Boolean {
        var prInEvent: TrackTopResult? = PRs.bestResults.firstOrNull { bestResult -> bestResult.event == trackResult.event }
        if (prInEvent == null) {
            if (runner.deviceId != null) {
                firebaseMessageService.sendMessageToDeviceId(runner.deviceId!!, "You ran a PR!",
                        "At " + meet.name + ", you competed in the " + trackResult.event +
                                " for the first time, and now have a PR of " + trackResult.time +
                                "! Way to go " + runner.name.split(" ").first() + "!", emptyMap())
                return true
            }
        } else {
            if (prInEvent.best.time.calculateSecondsFrom() > trackResult.time.calculateSecondsFrom()) {
                // set a new PR
                if (runner.deviceId != null) {
                    firebaseMessageService.sendMessageToDeviceId(runner.deviceId!!, "You ran a PR!",
                            "At " + meet.name + ", you set a new PR in the " + trackResult.event +
                                    " with a time of " + trackResult.time + " beating your previous best of " + prInEvent.best.time +
                                    "! Way to go " + runner.name.split(" ").first() + "!", emptyMap())
                    return true
                }
            }
        }
        return false
    }

    fun sendSBNotificationIfSB(runner: Runner, meet: TrackMeet, SBs: TrackPerformancesDTO, trackResult: TrackResult): Boolean {
        var sbInEvent: TrackTopResult? = SBs.bestResults.firstOrNull { bestResult -> bestResult.event == trackResult.event }
        if (sbInEvent == null) {
            if (runner.deviceId != null) {
                firebaseMessageService.sendMessageToDeviceId(runner.deviceId!!, "You ran a season best!",
                        "At " + meet.name + ", you competed in the " + trackResult.event +
                                " for the first time this season, and now have a season best of " + trackResult.time +
                                "! Way to go " + runner.name.split(" ").first() + "!", emptyMap())
                return true
            }
        } else {
            if (sbInEvent.best.time.calculateSecondsFrom() > trackResult.time.calculateSecondsFrom()) {
                // set a new PR
                if (runner.deviceId != null) {
                    firebaseMessageService.sendMessageToDeviceId(runner.deviceId!!, "You ran a season best!",
                            "At " + meet.name + ", you set a new season best in the " + trackResult.event +
                                    " with a time of " + trackResult.time + " beating your previous season best of " + sbInEvent.best.time +
                                    "! Way to go " + runner.name.split(" ").first() + "!", emptyMap())
                    return true
                }
            }
        }
        return false
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

