package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statistics.repository.MeetRepository
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
        val meetRepository: MeetRepository) {

    fun getMeetLog(meetId: String, runnerId: Int): MeetLogResponse {

        val meets = meetRepository.findByUuid(meetId)
        val log = meetLogRepository.findByMeetIdAndRunnerId(meetId, runnerId)

        return MeetLogResponse(log.firstOrNull(), meets.first())
    }

    fun getMeetLogsAtMeet(meetId: String): List<MeetLogResponse> {

        val meets = meetRepository.findByUuid(meetId)

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
            createMeetLogRequest.coolDownTime, createMeetLogRequest.coolDownPace, createMeetLogRequest.notes)

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

            meetLogRepository.save(log)

            return MeetLogResponse(log, meet)

        }
    }

    fun getAllMeetLogsForRunnerInSeason(runnerId: Int, season: String): List<Pair<MeetLogResponse, Date>> {
        return meetRepository.findByDateBetween(TimeUtilities.getFirstDayOfGivenYear(season), TimeUtilities.getLastDayOfGivenYear(season)).map {
            getMeetLog(it.uuid, runnerId) to it.date
        }.filter {it.first.meetLog != null}
        .map {
            it.first!! to it.second
        }
    }
}