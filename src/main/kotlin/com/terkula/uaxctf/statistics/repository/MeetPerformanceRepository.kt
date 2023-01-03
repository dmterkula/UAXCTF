package com.terkula.uaxctf.statistics.repository

import com.terkula.uaxctf.statisitcs.model.XCMeetPerformance
import org.springframework.data.repository.CrudRepository

interface MeetPerformanceRepository : CrudRepository<XCMeetPerformance, Int> {

    fun findByRunnerId(runnerId: Int?): MutableList<XCMeetPerformance>

    fun findByMeetId(meetId: Int): MutableList<XCMeetPerformance>

    fun findByMeetIdAndRunnerId(meetId: Int, runnerId: Int): XCMeetPerformance?

    fun findByRunnerIdAndPlace(runnerId: Int, place: Int): MutableList<XCMeetPerformance>


}
