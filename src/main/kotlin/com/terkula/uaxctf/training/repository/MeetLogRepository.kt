package com.terkula.uaxctf.training.repository

import com.terkula.uaxctf.training.model.MeetLog
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface MeetLogRepository: CrudRepository<MeetLog, Int> {

    fun findByMeetId(meetId: String): List<MeetLog>

    fun findByRunnerId(runnerId: Int): List<MeetLog>

    fun findByMeetIdAndRunnerId(meetId: String, runnerId: Int): List<MeetLog>

}