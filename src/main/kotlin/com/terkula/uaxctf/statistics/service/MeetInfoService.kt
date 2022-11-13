package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statisitcs.model.Meet
import com.terkula.uaxctf.statisitcs.model.MeetInfo
import com.terkula.uaxctf.statistics.dto.MeetToDatesRunDTO
import com.terkula.uaxctf.statistics.repository.MeetInfoRepository
import com.terkula.uaxctf.statistics.repository.MeetRepository
import com.terkula.uaxctf.util.TimeUtilities
import org.springframework.stereotype.Service
import java.sql.Date

@Service
class MeetInfoService(
    var meetRepository: MeetRepository,
    var meetInfoRepository: MeetInfoRepository
) {

    fun getMeetInfo(): List<MeetToDatesRunDTO>  {

        var meets = meetRepository.findAll()

        return meets.groupBy { it.name }.map {
            it.key to it.value.map { meet -> meet.date }
        }.map {
            MeetToDatesRunDTO(it.first, it.second)
        }
    }

    fun getMeetsForSeason(season: String): List<Meet> {
        return meetRepository.findByDateBetween(TimeUtilities.getFirstDayOfGivenYear(season), TimeUtilities.getLastDayOfGivenYear(season))
    }

    fun getMeetsBetweenDates(startDate: Date, endDate: Date): List<Meet> {
        return meetRepository.findByDateBetween(startDate, endDate)
    }
}