package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statisitcs.model.Meet
import com.terkula.uaxctf.statisitcs.model.MeetInfo
import com.terkula.uaxctf.statistics.repository.MeetInfoRepository
import com.terkula.uaxctf.statistics.repository.MeetRepository
import org.springframework.stereotype.Service

@Service
class MeetInfoService(
    var meetRepository: MeetRepository,
    var meetInfoRepository: MeetInfoRepository
) {

    fun getMeetInfo(): List<String>  {

        var meets = meetRepository.findAll()
        var meetInfo = meetInfoRepository.findAll()

        val meetsToId = meets.map { it.id to it }.toMap()
        val meetInfoToMeetId = meetInfo.map { it.meetId to it }.toMap()

        val meetsToInfos: Map<Meet, MeetInfo?> = meets.map { it to meetInfoToMeetId[it.id] }.toMap()

        return meets.map { it.name }.toSet().toList().sorted()
    }

}