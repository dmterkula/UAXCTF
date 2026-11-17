package com.terkula.uaxctf.weather

import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "event_weather", schema = "uaxc")
data class EventWeather(
    @Column(name = "event_type")
    var eventType: String,
    @Column(name = "event_id")
    var eventId: Int,
    var latitude: Double,
    var longitude: Double,
    @Column(name = "event_datetime")
    var eventDatetime: Timestamp,
    @Column(name = "temperature_c")
    var temperatureC: Double,
    @Column(name = "humidity_pct")
    var humidityPct: Double,
    var source: String,
    @Column(name = "fetched_at")
    var fetchedAt: Timestamp
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JoinColumn
    val id: Int = 0
}
