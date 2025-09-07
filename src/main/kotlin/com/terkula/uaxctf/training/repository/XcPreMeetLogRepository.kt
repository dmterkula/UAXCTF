package com.terkula.uaxctf.training.repository

import com.terkula.uaxctf.training.model.PreMeetLog
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface XcPreMeetLogRepository: CrudRepository<PreMeetLog, Int> {

    fun findByMeetId(meetId: String): List<PreMeetLog>

    fun findByRunnerId(runnerId: Int): List<PreMeetLog>

    fun findByMeetIdAndRunnerId(meetId: String, runnerId: Int): List<PreMeetLog>
}
