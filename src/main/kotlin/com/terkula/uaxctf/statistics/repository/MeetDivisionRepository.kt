package com.terkula.uaxctf.statistics.repository

import com.terkula.uaxctf.statisitcs.model.MeetDivision
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface MeetDivisionRepository : CrudRepository<MeetDivision, Int> {
    fun findByMeetIdOrderBySequence(meetId: Int): List<MeetDivision>
}
