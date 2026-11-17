package com.terkula.uaxctf.weather

import com.terkula.uaxctf.statisitcs.model.Venue
import com.terkula.uaxctf.statistics.repository.EventWeatherRepository
import org.springframework.stereotype.Service
import java.sql.Date
import java.sql.Time
import java.sql.Timestamp
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Resolves the temp/humidity to use for a heat-adjustment calculation for a given event,
 * fetching from Open-Meteo (via [WeatherService]) and caching past-dated lookups in
 * [EventWeatherRepository] so we don't re-hit the API on every view.
 */
@Service
class EventWeatherResolver(
    val weatherService: WeatherService,
    val eventWeatherRepository: EventWeatherRepository
) {

    fun resolve(eventType: String, eventId: Int, venue: Venue?, date: Date, startTime: Time?): ResolvedWeather? {
        // Indoor venues (e.g. a gym) aren't subject to outdoor heat/humidity, so never adjust for them.
        if (venue?.isIndoor == true) return null
        val latitude = venue?.latitude ?: return null
        val longitude = venue.longitude ?: return null
        val time = startTime?.toLocalTime() ?: defaultStartTime(eventType, date)
        val dateTime = LocalDateTime.of(date.toLocalDate(), time)

        val isPastEvent = dateTime.isBefore(LocalDateTime.now())
        if (isPastEvent) {
            val cached = eventWeatherRepository.findByEventTypeAndEventId(eventType, eventId)
            if (cached != null) {
                return ResolvedWeather(cached.temperatureC, cached.humidityPct, cached.source)
            }
        }

        val fetched = weatherService.getWeatherFor(latitude, longitude, dateTime) ?: return null

        if (isPastEvent) {
            eventWeatherRepository.save(
                EventWeather(
                    eventType = eventType,
                    eventId = eventId,
                    latitude = latitude,
                    longitude = longitude,
                    eventDatetime = Timestamp.valueOf(dateTime),
                    temperatureC = fetched.temperatureC,
                    humidityPct = fetched.humidityPct,
                    source = fetched.source,
                    fetchedAt = Timestamp(System.currentTimeMillis())
                )
            )
        }

        return fetched
    }

    /**
     * Track meets have no start-time picker (a meet can run for hours, so there's no single
     * representative time) — so we guess from the day of week instead: weekday meets are after
     * school (4:30pm), weekend meets/invitationals typically kick off mid-morning (10:00am).
     * Every other event type just uses the standard practice-time default.
     */
    private fun defaultStartTime(eventType: String, date: Date): LocalTime {
        if (eventType == EVENT_TYPE_TRACK_MEET) {
            val dayOfWeek = date.toLocalDate().dayOfWeek
            if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
                return WEEKEND_TRACK_MEET_START_TIME
            }
        }
        return DEFAULT_START_TIME
    }

    companion object {
        // Default practice/weekday-meet start time — used only when no start time is set for an event.
        val DEFAULT_START_TIME: LocalTime = LocalTime.of(16, 30)
        val WEEKEND_TRACK_MEET_START_TIME: LocalTime = LocalTime.of(10, 0)

        const val EVENT_TYPE_MEET = "MEET"
        const val EVENT_TYPE_MEET_DIVISION = "MEET_DIVISION"
        const val EVENT_TYPE_TRACK_MEET = "TRACK_MEET"
        const val EVENT_TYPE_WORKOUT = "WORKOUT"
        const val EVENT_TYPE_TRAINING_RUN = "TRAINING_RUN"
    }
}
