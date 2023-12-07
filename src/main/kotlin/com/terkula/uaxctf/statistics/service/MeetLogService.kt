package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statisitcs.model.Meet
import com.terkula.uaxctf.statistics.repository.MeetRepository
import com.terkula.uaxctf.statistics.repository.track.TrackMeetRepository
import com.terkula.uaxctf.statistics.request.CreateMeetLogRequest
import com.terkula.uaxctf.training.model.MeetLog
import com.terkula.uaxctf.training.repository.MeetLogRepository
import com.terkula.uaxctf.training.response.MeetLogResponse
import com.terkula.uaxctf.util.TimeUtilities
import org.springframework.stereotype.Service
import java.sql.Date

@Service
class MeetLogService (
        val meetLogRepository: MeetLogRepository,
        val meetRepository: MeetRepository,
        val trackMeetRepository: TrackMeetRepository
) {

    fun getMeetLog(meetId: String, runnerId: Int): MeetLogResponse {

        val meets = meetRepository.findByUuid(meetId)

        val trackMeet = trackMeetRepository.findByUuid(meetId)
        if (trackMeet.isPresent) {
            val result = trackMeet.get()
            meets.add(Meet(result.name, result.date, result.uuid, result.icon))
        }

        val log = meetLogRepository.findByMeetIdAndRunnerId(meetId, runnerId)

        return MeetLogResponse(log.firstOrNull(), meets.first())
    }

    fun getMeetLogsAtMeet(meetId: String): List<MeetLogResponse> {

        val meets = meetRepository.findByUuid(meetId)
        val trackMeet = trackMeetRepository.findByUuid(meetId)
        if (trackMeet.isPresent) {
            val result = trackMeet.get()
            meets.add(Meet(result.name, result.date, result.uuid, result.icon))
        }

        return meetLogRepository.findByMeetId(meetId).map{
            MeetLogResponse(it, meets.first())
        }
    }

    fun createMeetLog(createMeetLogRequest: CreateMeetLogRequest): MeetLogResponse {

        val meet = meetRepository.findByUuid(createMeetLogRequest.meetId).first()
        val existingLog = meetLogRepository.findByMeetIdAndRunnerId(createMeetLogRequest.meetId, createMeetLogRequest.runnerId)

        if (existingLog.firstOrNull() == null) {

            val newMeetLog = MeetLog(createMeetLogRequest.meetId, createMeetLogRequest.runnerId, createMeetLogRequest.time,
                    createMeetLogRequest.warmUpDistance, createMeetLogRequest.warmUpTime, createMeetLogRequest.warmUpPace, createMeetLogRequest.coolDownDistance,
                    createMeetLogRequest.coolDownTime, createMeetLogRequest.coolDownPace, createMeetLogRequest.notes, createMeetLogRequest.coachNotes, createMeetLogRequest.season)

            meetLogRepository.save(newMeetLog)

            return MeetLogResponse(newMeetLog, meet)

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

            meetLogRepository.save(log)

            return MeetLogResponse(log, meet)

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
            }.filter {it.first.meetLog != null}
                    .map {
                        it.first!! to it.second
                    }
        }

    }
}