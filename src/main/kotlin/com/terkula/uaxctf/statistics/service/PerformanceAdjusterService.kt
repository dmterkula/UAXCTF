package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statistics.dto.MeetPerformanceDTO
import com.terkula.uaxctf.statistics.dto.adjustForDistance
import com.terkula.uaxctf.statistics.repository.MeetInfoRepository
import com.terkula.uaxctf.statistics.repository.MeetRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PerformanceAdjusterService (@field: Autowired
                                  internal var meetInfoRepository: MeetInfoRepository,
                                  @field: Autowired
                                  internal var meetRepository: MeetRepository) {

    fun adjustMeetPerformances(performances: List<MeetPerformanceDTO>, adjustForDistance: Boolean) : List<MeetPerformanceDTO> {

        if (!adjustForDistance) {
            return performances
        }
        return adjustForDistance(performances)
    }

    private fun adjustForDistance(performances: List<MeetPerformanceDTO>): List<MeetPerformanceDTO> {
        return performances.map {
            val meet = meetRepository.findByNameAndDate(it.meetName, it.meetDate)
            if (meet == null) {
                return@map it
            } else {
                val meetInfo = meetInfoRepository.findByMeetId(meet.id)
                if (meetInfo == null) {
                    return@map it
                } else {
                    return@map it.adjustForDistance(meetInfo.distance)
                }
            }
        }
    }
}