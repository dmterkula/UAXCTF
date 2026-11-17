package com.terkula.uaxctf.statistics.repository

import com.terkula.uaxctf.weather.EventWeather
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface EventWeatherRepository : CrudRepository<EventWeather, Int> {
    fun findByEventTypeAndEventId(eventType: String, eventId: Int): EventWeather?
}
