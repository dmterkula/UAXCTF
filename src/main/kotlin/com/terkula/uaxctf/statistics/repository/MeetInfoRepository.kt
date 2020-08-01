package com.terkula.uaxctf.statistics.repository

import com.terkula.uaxctf.statisitcs.model.MeetInfo
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface MeetInfoRepository: CrudRepository<MeetInfo, Int> {

    fun findByMeetId(meetId: Int): MeetInfo?

}