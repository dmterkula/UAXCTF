package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statisitcs.model.Meet
import com.terkula.uaxctf.statisitcs.model.MeetInfo
import com.terkula.uaxctf.statisitcs.model.track.TrackMeet
import com.terkula.uaxctf.statistics.dto.MeetToDatesRunDTO
import com.terkula.uaxctf.statistics.repository.MeetInfoRepository
import com.terkula.uaxctf.statistics.repository.MeetRepository
import com.terkula.uaxctf.statistics.repository.track.TrackMeetRepository
import com.terkula.uaxctf.statistics.request.CreateMeetRequest
import com.terkula.uaxctf.util.TimeUtilities
import org.springframework.stereotype.Service
import java.sql.Date
import java.util.*

@Service
class MeetInfoService(
    var meetRepository: MeetRepository,
    var trackMeetRepository: TrackMeetRepository,
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

    fun getTrackMeetInfo(): List<MeetToDatesRunDTO>  {

        var meets = trackMeetRepository.findAll()

        return meets.groupBy { it.name }.map {
            it.key to it.value.map { meet -> meet.date }
        }.map {
            MeetToDatesRunDTO(it.first, it.second)
        }
    }

    fun getMeetsForSeason(season: String): List<Meet> {

        val meets =
        meetRepository.findByDateBetween(TimeUtilities.getFirstDayOfGivenYear(season), TimeUtilities.getLastDayOfGivenYear(season))

        meets.forEach { it.uuid = it.uuid.uppercase(Locale.getDefault()) }

        return meets
    }

    fun getTrackMeetsForSeason(season: String): List<TrackMeet> {

        val meets =
                trackMeetRepository.findByDateBetween(TimeUtilities.getFirstDayOfGivenYear(season), TimeUtilities.getLastDayOfGivenYear(season))

        meets.forEach { it.uuid = it.uuid.uppercase(Locale.getDefault()) }

        return meets
    }

    fun getMeetsBetweenDates(startDate: Date, endDate: Date): List<Meet> {

        val meets = meetRepository.findByDateBetween(startDate, endDate)

        meets.forEach { it.uuid = it.uuid.uppercase(Locale.getDefault()) }

        return meets
    }

    fun getTrackMeetsBetweenDates(startDate: Date, endDate: Date): List<TrackMeet> {

        val meets = trackMeetRepository.findByDateBetween(startDate, endDate)

        meets.forEach { it.uuid = it.uuid.uppercase(Locale.getDefault()) }

        return meets
    }

    fun createMeet(createMeetRequest: CreateMeetRequest): Meet {

        val meet = Meet(createMeetRequest.name, createMeetRequest.date, createMeetRequest.uuid, createMeetRequest.icon)

        return meetRepository.save(meet)
    }

    fun createTrackMeet(createMeetRequest: CreateMeetRequest): TrackMeet {

        val meet = TrackMeet(createMeetRequest.name, createMeetRequest.uuid, createMeetRequest.date, createMeetRequest.icon)

        return trackMeetRepository.save(meet)
    }

    fun updateMeet(createMeetRequest: CreateMeetRequest): Meet {

        val meets = meetRepository.findByUuid(createMeetRequest.uuid)

        if (meets.isEmpty()) {
            return createMeet(createMeetRequest)
        } else {
            val meet = meets.first()
            meet.date = createMeetRequest.date
            meet.name = createMeetRequest.name
            meet.icon = createMeetRequest.icon

            return meetRepository.save(meet)
        }
    }

    fun updateTrackMeet(createMeetRequest: CreateMeetRequest): TrackMeet {

        val meets = trackMeetRepository.findByUuid(createMeetRequest.uuid)

        if (!meets.isPresent) {
            return createTrackMeet(createMeetRequest)
        } else {
            val meet = meets.get()
            meet.date = createMeetRequest.date
            meet.name = createMeetRequest.name
            meet.icon = createMeetRequest.icon

            return trackMeetRepository.save(meet)
        }
    }
}