package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statistics.repository.MeetRepository
import com.terkula.uaxctf.statistics.request.CreateMeetLogRequest
import com.terkula.uaxctf.training.model.MeetLog
import com.terkula.uaxctf.training.repository.MeetLogRepository
import com.terkula.uaxctf.util.TimeUtilities
import org.springframework.stereotype.Service
import java.sql.Date

@Service
class MeetLogService (
        val meetLogRepository: MeetLogRepository,
        val meetRepository: MeetRepository) {

    fun getMeetLog(meetId: Int, runnerId: Int): MeetLog? {

        val log = meetLogRepository.findByMeetIdAndRunnerId(meetId, runnerId)
        return log.firstOrNull()
    }

    fun createMeetLog(createMeetLogRequest: CreateMeetLogRequest): MeetLog {

        val existingLog = meetLogRepository.findByMeetIdAndRunnerId(createMeetLogRequest.meetId, createMeetLogRequest.runnerId)

        if (existingLog.firstOrNull() == null) {

            val newMeetLog = MeetLog(createMeetLogRequest.meetId, createMeetLogRequest.runnerId, createMeetLogRequest.time,
            createMeetLogRequest.warmUpDistance, createMeetLogRequest.warmUpTime, createMeetLogRequest.warmUpPace, createMeetLogRequest.coolDownDistance,
            createMeetLogRequest.coolDownTime, createMeetLogRequest.coolDownPace, createMeetLogRequest.notes)

            meetLogRepository.save(newMeetLog)

            return newMeetLog

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

            return log

        }
    }

    fun getAllMeetLogsForRunnerInSeason(runnerId: Int, season: String): List<Pair<MeetLog, Date>> {
        return meetRepository.findByDateBetween(TimeUtilities.getFirstDayOfGivenYear(season), TimeUtilities.getLastDayOfGivenYear(season)).map {
            getMeetLog(it.id, runnerId) to it.date
        }.filter {it.first != null}
        .map {
            it.first!! to it.second
        }
    }
}