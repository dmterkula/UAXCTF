package com.terkula.uaxctf.training.repository

import com.terkula.uaxctf.training.model.PreMeetLog
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface XcPreMeetLogRepository: CrudRepository<PreMeetLog, Int> {

    fun findByMeetId(meetId: String): List<PreMeetLog>

    /** Batch variant, so a date range of meets costs one query instead of one per meet. */
    fun findByMeetIdIn(meetIds: List<String>): List<PreMeetLog>

    fun findByRunnerId(runnerId: Int): List<PreMeetLog>

    fun findByMeetIdAndRunnerId(meetId: String, runnerId: Int): List<PreMeetLog>
}
