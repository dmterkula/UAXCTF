package com.terkula.uaxctf.statistics.repository.track

import com.terkula.uaxctf.training.model.TrackMeetLog
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface TrackMeetLogRepository: CrudRepository<TrackMeetLog, Int> {

    fun findByMeetId(meetId: String): List<TrackMeetLog>

    fun findByRunnerId(runnerId: Int): List<TrackMeetLog>

    fun findByMeetIdAndRunnerId(meetId: String, runnerId: Int): List<TrackMeetLog>

}