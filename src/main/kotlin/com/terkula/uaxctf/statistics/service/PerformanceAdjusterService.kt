package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statistics.dto.MeetPerformanceDTO
import com.terkula.uaxctf.statistics.dto.adjustForDistance
import com.terkula.uaxctf.statistics.dto.adjustForHeat
import com.terkula.uaxctf.statistics.repository.MeetInfoRepository
import com.terkula.uaxctf.statistics.repository.MeetRepository
import com.terkula.uaxctf.statistics.repository.VenueRepository
import com.terkula.uaxctf.statistics.repository.MeetDivisionRepository
import com.terkula.uaxctf.weather.EventWeatherResolver
import com.terkula.uaxctf.weather.HeatPaceAdjustmentService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PerformanceAdjusterService (@field: Autowired
                                  internal var meetInfoRepository: MeetInfoRepository,
                                  @field: Autowired
                                  internal var meetRepository: MeetRepository,
                                  @field: Autowired
                                  internal var venueRepository: VenueRepository,
                                  @field: Autowired
                                  internal var eventWeatherResolver: EventWeatherResolver,
                                  @field: Autowired
                                  internal var heatPaceAdjustmentService: HeatPaceAdjustmentService,
                                  @field: Autowired
                                  internal var meetDivisionRepository: MeetDivisionRepository) {

    fun adjustMeetPerformances(performances: List<MeetPerformanceDTO>, adjustForDistance: Boolean, adjustForHeat: Boolean = false) : List<MeetPerformanceDTO> {

        var adjusted = if (adjustForDistance) adjustForDistance(performances) else performances
        if (adjustForHeat) {
            adjusted = withHeatAdjustment(adjusted)
        }
        return adjusted
    }

    /**
     * Populates [MeetPerformanceDTO.heatAdjustedTime] with the flat/ideal-condition equivalent time
     * for each performance, using weather auto-fetched for the meet's venue + date/start time. Leaves
     * performances untouched (heatAdjustedTime stays null) when venue coordinates or weather aren't
     * resolvable. When the performance has a division with its own start time (e.g. an early-morning
     * Open race vs. an evening Varsity race at the same meet), that start time is used instead of the
     * meet-level one, and weather is cached per-division rather than per-meet.
     */
    fun withHeatAdjustment(performances: List<MeetPerformanceDTO>): List<MeetPerformanceDTO> {
        return performances.map {
            val meet = meetRepository.findByNameAndDate(it.meetName, it.meetDate) ?: return@map it
            val venue = meet.venueId?.let { id -> venueRepository.findById(id).orElse(null) }
            val division = it.divisionId?.let { id -> meetDivisionRepository.findById(id).orElse(null) }

            val weather = if (division != null) {
                eventWeatherResolver.resolve(
                    EventWeatherResolver.EVENT_TYPE_MEET_DIVISION, division.id, venue, meet.date, division.startTime ?: meet.startTime
                )
            } else {
                eventWeatherResolver.resolve(
                    EventWeatherResolver.EVENT_TYPE_MEET, meet.id, venue, meet.date, meet.startTime
                )
            } ?: return@map it

            it.adjustForHeat(weather.temperatureC, weather.humidityPct, heatPaceAdjustmentService)
        }
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