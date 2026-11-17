package com.terkula.uaxctf.statistics.repository.track

import com.terkula.uaxctf.training.model.TrackMeetLog
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface TrackMeetLogRepository: CrudRepository<TrackMeetLog, Int> {

    fun findByMeetId(meetId: String): List<TrackMeetLog>

    /** Batch variant, so a date range of meets costs one query instead of one per meet. */
    fun findByMeetIdIn(meetIds: List<String>): List<TrackMeetLog>

    fun findByRunnerId(runnerId: Int): List<TrackMeetLog>

    fun findByMeetIdAndRunnerId(meetId: String, runnerId: Int): List<TrackMeetLog>

}