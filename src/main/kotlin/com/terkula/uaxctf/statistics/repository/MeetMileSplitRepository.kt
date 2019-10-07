package com.terkula.uaxctf.statistics.repository

import com.terkula.uaxctf.statisitcs.model.MeetMileSplit
import org.springframework.data.repository.CrudRepository

interface MeetMileSplitRepository: CrudRepository<MeetMileSplit, Int> {

    fun findByMeetId(meetId: Int): List<MeetMileSplit>
    fun findByMeetIdAndRunnerId(meetId: Int, runnerId: Int): List<MeetMileSplit>
}