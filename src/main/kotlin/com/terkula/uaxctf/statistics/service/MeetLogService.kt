package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statisitcs.model.Meet
import com.terkula.uaxctf.statisitcs.model.track.TrackMeet
import com.terkula.uaxctf.statistics.repository.MeetRepository
import com.terkula.uaxctf.statistics.repository.track.TrackMeetLogRepository
import com.terkula.uaxctf.statistics.repository.track.TrackMeetRepository
import com.terkula.uaxctf.statistics.request.CreateMeetLogRequest
import com.terkula.uaxctf.statistics.request.CreateXcPreMeetLogRequest
import com.terkula.uaxctf.statistics.request.track.CreateTrackMeetLogRequest
import com.terkula.uaxctf.training.model.MeetLog
import com.terkula.uaxctf.training.model.TrackMeetLog
import com.terkula.uaxctf.training.model.PreMeetLog
import com.terkula.uaxctf.training.model.TrainingComment
import com.terkula.uaxctf.training.repository.MeetLogRepository
import com.terkula.uaxctf.training.repository.TrainingCommentRepository
import com.terkula.uaxctf.training.repository.XcPreMeetLogRepository
import com.terkula.uaxctf.training.response.MeetLogResponse
import com.terkula.uaxctf.training.response.PreMeetLogResponse
import com.terkula.uaxctf.training.response.track.TrackMeetLogResponse
import org.springframework.stereotype.Service
import java.sql.Date

@Service
class MeetLogService (
        val meetLogRepository: MeetLogRepository,
        val meetRepository: MeetRepository,
        val trackMeetLogRepository: TrackMeetLogRepository,
        val trackMeetRepository: TrackMeetRepository,
        val xcPreMeetLogRepository: XcPreMeetLogRepository,
        val commentRepository: TrainingCommentRepository
) {

    fun getRunnerMeetLogsBetweenDates(runnerId: Int, startDate: Date, endDate: Date): Pair<List<MeetLogResponse>, List<TrackMeetLogResponse>> {
        val meets = meetRepository.findByDateBetween(startDate, endDate).map {
            getMeetLog(it.uuid, runnerId)
        }
        val trackMeets = trackMeetRepository.findByDateBetween(startDate, endDate)
                .map{
                    getTrackMeetLogs(it.uuid, runnerId)
                }

        return Pair(meets, trackMeets)

    }

    fun getMeetLog(meetId: String, runnerId: Int): MeetLogResponse {

        val meets = meetRepository.findByUuid(meetId)

        val trackMeet = trackMeetRepository.findByUuid(meetId)
        if (trackMeet.isPresent) {
            val result = trackMeet.get()
            meets.add(Meet(result.name, result.date, result.uuid, result.icon, result.team))
        }

        val log = meetLogRepository.findByMeetIdAndRunnerId(meetId, runnerId)
        val preMeetLog: PreMeetLog? = xcPreMeetLogRepository.findByMeetIdAndRunnerId(meetId, runnerId).firstOrNull()
        var preMeetComments = emptyList<TrainingComment>()
        var postMeetComments = emptyList<TrainingComment>()

        if (preMeetLog != null) {
            preMeetComments = commentRepository.findByTrainingEntityUuid(preMeetLog!!.uuid)
        }
        if (log.firstOrNull() != null) {
            postMeetComments = commentRepository.findByTrainingEntityUuid(log.first().meetId + "-" + log.first().runnerId)
        }

        return MeetLogResponse(log.firstOrNull(), meets.first(), preMeetLog, preMeetComments, postMeetComments)
    }

    fun getXcPreMeetLog(meetId: String, runnerId: Int): PreMeetLogResponse{

        val meets = meetRepository.findByUuid(meetId)

        val trackMeet = trackMeetRepository.findByUuid(meetId)
        if (trackMeet.isPresent) {
            val result = trackMeet.get()
            meets.add(Meet(result.name, result.date, result.uuid, result.icon, result.team))
        }

        val log = xcPreMeetLogRepository.findByMeetIdAndRunnerId(meetId, runnerId)

        return PreMeetLogResponse(log.firstOrNull(), meets.first())
    }

    fun createXcPreMeetLog(createMeetLogRequest: CreateXcPreMeetLogRequest): MeetLogResponse {

        val meet = meetRepository.findByUuid(createMeetLogRequest.meetId).first()
        val existingPreMeetLog = xcPreMeetLogRepository.findByMeetIdAndRunnerId(createMeetLogRequest.meetId, createMeetLogRequest.runnerId)

        val existingMeetLog = meetLogRepository.findByMeetIdAndRunnerId(createMeetLogRequest.meetId, createMeetLogRequest.runnerId).firstOrNull()

        if (existingPreMeetLog.firstOrNull() == null) {

            val newPreMeetLog = PreMeetLog(createMeetLogRequest.meetId, createMeetLogRequest.runnerId, createMeetLogRequest.goals,
            createMeetLogRequest.plan, createMeetLogRequest.confidence, createMeetLogRequest.preparation, createMeetLogRequest.whenItsHard,
            createMeetLogRequest.questions, createMeetLogRequest.notes, createMeetLogRequest.sleepScore, createMeetLogRequest.fuelingScore,
            createMeetLogRequest.hydrationScore, createMeetLogRequest.sorenessScore, createMeetLogRequest.uuid)

            xcPreMeetLogRepository.save(newPreMeetLog)

            return MeetLogResponse(existingMeetLog, meet, newPreMeetLog, emptyList(), commentRepository.findByTrainingEntityUuid(createMeetLogRequest.meetId + "-" + createMeetLogRequest.runnerId))

        } else {
            val preMeetLog = existingPreMeetLog.first()

            preMeetLog.goals = createMeetLogRequest.goals
            preMeetLog.plan = createMeetLogRequest.plan
            preMeetLog.confidence = createMeetLogRequest.confidence
            preMeetLog.preparation = createMeetLogRequest.preparation
            preMeetLog.whenItsHard = createMeetLogRequest.whenItsHard
            preMeetLog.questions = createMeetLogRequest.questions
            preMeetLog.notes = createMeetLogRequest.notes
            preMeetLog.sleepScore = createMeetLogRequest.sleepScore
            preMeetLog.fuelingScore = createMeetLogRequest.fuelingScore
            preMeetLog.hydrationScore = createMeetLogRequest.hydrationScore
            preMeetLog.sorenessScore = createMeetLogRequest.sorenessScore


            xcPreMeetLogRepository.save(preMeetLog)

            var preMeetComments: List<TrainingComment> = commentRepository.findByTrainingEntityUuid(createMeetLogRequest.uuid)
            var postMeetComments: List<TrainingComment> = commentRepository.findByTrainingEntityUuid(createMeetLogRequest.meetId + "-" + createMeetLogRequest.runnerId)

            return MeetLogResponse(existingMeetLog, meet, preMeetLog, preMeetComments, postMeetComments)

        }
    }


    fun getTrackMeetLogs(meetId: String, runnerId: Int): TrackMeetLogResponse {

        val meets = trackMeetRepository.findByUuid(meetId)
        val logs = trackMeetLogRepository.findByMeetIdAndRunnerId(meetId, runnerId)

        return TrackMeetLogResponse(logs, meets.get())
    }

    fun getMeetLogsAtMeet(meetId: String): List<MeetLogResponse> {

        val meets = meetRepository.findByUuid(meetId)
        val trackMeet = trackMeetRepository.findByUuid(meetId)
        if (trackMeet.isPresent) {
            val result = trackMeet.get()
            meets.add(Meet(result.name, result.date, result.uuid, result.icon, result.team))
        }

        val preMeetLogs = xcPreMeetLogRepository.findByMeetId(meetId).map { it.runnerId to it }.toMap()

        return meetLogRepository.findByMeetId(meetId).map{
            var preMeetComments: List<TrainingComment> = emptyList()
            val preMeetLog = preMeetLogs[it.runnerId]
            if (preMeetLog != null) {
                preMeetComments = commentRepository.findByTrainingEntityUuid(preMeetLog!!.uuid)
                        .sortedBy { it.timestamp }
            }

            val postMeetComments: List<TrainingComment> = commentRepository.findByTrainingEntityUuid(it.meetId + "-" + it.runnerId)
                    .sortedBy { it.timestamp }

            MeetLogResponse(it, meets.first(), preMeetLogs[it.runnerId], preMeetComments, postMeetComments)
        }
    }

    fun getTrackMeetLogsAtMeet(meetId: String): List<TrackMeetLogResponse> {

        val meets = trackMeetRepository.findByUuid(meetId)

       return trackMeetLogRepository.findByMeetId(meetId).groupBy { it.runnerId }
                .map {
                    TrackMeetLogResponse(it.value, meets.get())
                }
    }

    fun createMeetLog(createMeetLogRequest: CreateMeetLogRequest): MeetLogResponse {

        val meet = meetRepository.findByUuid(createMeetLogRequest.meetId).first()
        val existingLog = meetLogRepository.findByMeetIdAndRunnerId(createMeetLogRequest.meetId, createMeetLogRequest.runnerId)
        val preMeetLog = xcPreMeetLogRepository.findByMeetIdAndRunnerId(createMeetLogRequest.meetId, createMeetLogRequest.runnerId).firstOrNull()

        var preMeetComments = emptyList<TrainingComment>()
        var postMeetComments = emptyList<TrainingComment>()

        if (preMeetLog != null) {
            preMeetComments = commentRepository.findByTrainingEntityUuid(preMeetLog!!.uuid)
        }
        if (existingLog.firstOrNull() != null) {
            postMeetComments = commentRepository.findByTrainingEntityUuid(existingLog.first().meetId + "-" + existingLog.first().runnerId)
        }

        if (existingLog.firstOrNull() == null) {

            val newMeetLog = MeetLog(createMeetLogRequest.meetId, createMeetLogRequest.runnerId, createMeetLogRequest.time,
                    createMeetLogRequest.warmUpDistance, createMeetLogRequest.warmUpTime, createMeetLogRequest.warmUpPace, createMeetLogRequest.coolDownDistance,
                    createMeetLogRequest.coolDownTime, createMeetLogRequest.coolDownPace, createMeetLogRequest.notes, createMeetLogRequest.coachNotes, createMeetLogRequest.season,
                    createMeetLogRequest.satisfaction, createMeetLogRequest.happyWith, createMeetLogRequest.notHappyWith
            )



            meetLogRepository.save(newMeetLog)

            return MeetLogResponse(newMeetLog, meet, preMeetLog, preMeetComments, postMeetComments)

        } else {
            val log = existingLog.first()

            log.time = createMeetLogRequest.time
            log.warmUpDistance = createMeetLogRequest.warmUpDistance
            log.warmUpTime = createMeetLogRequest.warmUpTime
            log.warmUpPace = createMeetLogRequest.warmUpPace
            log.coolDownDistance = createMeetLogRequest.coolDownDistance
            log.coolDownTime = createMeetLogRequest.coolDownTime
            log.coolDownPace = createMeetLogRequest.coolDownPace
            log.notes = createMeetLogRequest.notes
            log.coachNotes = createMeetLogRequest.coachNotes
            log.season = createMeetLogRequest.season
            log.satisfaction = createMeetLogRequest.satisfaction
            log.happyWith = createMeetLogRequest.happyWith
            log.notHappyWith = createMeetLogRequest.notHappyWith

            meetLogRepository.save(log)

            return MeetLogResponse(log, meet, preMeetLog, preMeetComments, postMeetComments)

        }
    }

        fun createTrackMeetLogs(createTrackMeetLogRequest: CreateTrackMeetLogRequest): TrackMeetLogResponse {

            if (createTrackMeetLogRequest.logs.isEmpty()) {
                return TrackMeetLogResponse(emptyList(), null)
            }

            val meet: TrackMeet = trackMeetRepository.findByUuid(createTrackMeetLogRequest.logs.first().meetId).get()
            val existingLogs = trackMeetLogRepository.findByMeetIdAndRunnerId(createTrackMeetLogRequest.logs.first().meetId, createTrackMeetLogRequest.logs.first().runnerId )

            if (existingLogs.firstOrNull() == null) {

                val logs = mutableListOf<TrackMeetLog>()

                createTrackMeetLogRequest.logs.forEach {
                    val newMeetLog = TrackMeetLog(createTrackMeetLogRequest.logs.first().meetId, createTrackMeetLogRequest.logs.first().runnerId, it.time,
                            it.warmUpDistance, it.warmUpTime, it.warmUpPace, it.coolDownDistance,
                            it.coolDownTime, it.coolDownPace, it.notes, it.coachNotes, it.season, it.event)

                    logs.add(newMeetLog)

                    trackMeetLogRepository.save(newMeetLog)

                }

                return TrackMeetLogResponse(logs, meet)


            } else {
                // have at least a partial update
                val logs = mutableListOf<TrackMeetLog>()

                createTrackMeetLogRequest.logs.forEach {

                    val existingLogForEvent = existingLogs.firstOrNull { existingLog -> existingLog.event == it.event }
                    if (existingLogForEvent == null) {
                        // creating log for event for the first time
                        val newMeetLog = TrackMeetLog(createTrackMeetLogRequest.logs.first().meetId, createTrackMeetLogRequest.logs.first().runnerId, it.time,
                                it.warmUpDistance, it.warmUpTime, it.warmUpPace, it.coolDownDistance,
                                it.coolDownTime, it.coolDownPace, it.notes, it.coachNotes, it.season, it.event)

                        logs.add(newMeetLog)

                        trackMeetLogRepository.save(newMeetLog)
                    } else {
                        // override existing log
                        existingLogForEvent.time = it.time
                        existingLogForEvent.warmUpDistance = it.warmUpDistance
                        existingLogForEvent.warmUpTime = it.warmUpTime
                        existingLogForEvent.warmUpPace = it.warmUpPace
                        existingLogForEvent.coolDownTime = it.coolDownTime
                        existingLogForEvent.coolDownDistance = it.coolDownDistance
                        existingLogForEvent.coolDownPace = it.coolDownPace
                        existingLogForEvent.notes = it.notes
                        existingLogForEvent.coachNotes = it.coachNotes
                        existingLogForEvent.season = it.season
                        existingLogForEvent.event = it.event

                        logs.add(existingLogForEvent)
                        trackMeetLogRepository.save(existingLogForEvent)
                    }

                }

                return TrackMeetLogResponse(logs, meet)
            }
    }

    fun getAllMeetLogsForRunnerInSeason(runnerId: Int, startDate: Date, endDate: Date, type: String): List<Pair<MeetLogResponse, Date>> {

        return if (type.equals("track", ignoreCase = true)) {
            trackMeetRepository.findByDateBetween(startDate, endDate).map {
                getMeetLog(it.uuid, runnerId) to it.date
            }.filter {it.first.meetLog != null}
                    .map {
                        it.first!! to it.second
                    }
        } else {
            meetRepository.findByDateBetween(startDate, endDate).map {
                getMeetLog(it.uuid, runnerId) to it.date
            }.filter {it.first.meetLog != null || it.first.preMeetLog != null}
                    .map {
                        it.first!! to it.second
                    }
        }

    }

    fun getAllTrackMeetLogsForRunnerInSeason(runnerId: Int, startDate: Date, endDate: Date, type: String): List<Pair<TrackMeetLogResponse, Date>> {

        return trackMeetRepository.findByDateBetween(startDate, endDate)
                .map { getTrackMeetLogs(it.uuid, runnerId) to it.date }
                .filter { it.first.trackMeetLogs.isNotEmpty() }
                .map { it.first to it.second }
    }
}