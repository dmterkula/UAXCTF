package com.terkula.uaxctf.statistics.repository.track

import com.terkula.uaxctf.statisitcs.model.track.TrackMeetPerformance
import org.springframework.data.repository.CrudRepository
import org.springframework.transaction.annotation.Transactional

@Transactional
interface TrackMeetPerformanceRepository : CrudRepository<TrackMeetPerformance, Int> {

    fun findByRunnerId(runnerId: Int?): MutableList<TrackMeetPerformance>

    fun findByRunnerIdAndEvent(runnerId: Int?, event: String): MutableList<TrackMeetPerformance>

    fun findByMeetId(meetId: String): MutableList<TrackMeetPerformance>

    fun findByMeetIdAndRunnerId(meetId: String, runnerId: Int): List<TrackMeetPerformance>

    fun deleteAllByRunnerId(runnerId: Int)

}
