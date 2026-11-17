package com.terkula.uaxctf.weather

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class ResolvedWeather(val temperatureC: Double, val humidityPct: Double, val source: String)

data class GeocodeResult(val latitude: Double, val longitude: Double)

/**
 * Fetches temp/humidity for a venue + point in time via Open-Meteo (free, keyless).
 * Forecast covers roughly the next 16 days; anything further out or in the past falls
 * back to the historical archive endpoint.
 */
@Service
class WeatherService {

    private val restTemplate = RestTemplate()
    // Open-Meteo responses carry lots of fields we don't declare (generationtime_ms, elevation,
    // timezone, admin1/2/3, etc.) — ignore anything we don't map instead of failing to parse.
    private val objectMapper = ObjectMapper().registerKotlinModule()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    private val log = LoggerFactory.getLogger(WeatherService::class.java)

    /**
     * Open-Meteo's geocoder is a place-name gazetteer, not a street-address geocoder, so a full
     * street address (e.g. "3947 Broadway, Bronx, NY") usually won't match. We try the address as
     * given first, then fall back to progressively coarser trailing chunks (e.g. "Bronx, NY", then
     * "NY") — city-level accuracy is more than sufficient for temp/humidity. A trailing ZIP code
     * (e.g. "Sharonville, OH 45241") also breaks every candidate, including the coarse ones, so it's
     * stripped up front rather than just from the full address.
     */
    fun geocode(address: String): GeocodeResult? {
        val withoutZip = address.replace(Regex("\\s*\\d{5}(-\\d{4})?\\s*$"), "")
        val parts = withoutZip.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val candidates = (listOf(withoutZip) + (1 until parts.size).map { parts.drop(it).joinToString(", ") })
            // A bare 2-letter code (a state/country abbreviation left over once every more specific
            // candidate has failed) is too ambiguous to trust -- it can match an unrelated place
            // anywhere in the world (e.g. "OH" matched a village in Switzerland, not Ohio) rather than
            // the state it was meant to narrow down. Better to return no coordinates than wrong ones.
            .filterNot { it.trim().matches(Regex("^[A-Za-z]{2}$")) }

        for (candidate in candidates) {
            geocodeExact(candidate)?.let { return it }
        }
        return null
    }

    private fun geocodeExact(query: String): GeocodeResult? {
        return try {
            // Build via UriComponentsBuilder and pass a URI (not a String) to getForObject —
            // RestTemplate treats String URLs as templates and re-encodes them, which double-encodes
            // any character we already escaped (e.g. a "," in the address becomes "%2C" then "%252C").
            val uri = org.springframework.web.util.UriComponentsBuilder
                .fromHttpUrl("https://geocoding-api.open-meteo.com/v1/search")
                .queryParam("name", query)
                .queryParam("count", 1)
                .build()
                .toUri()
            val body = restTemplate.getForObject(uri, String::class.java) ?: return null
            val response = objectMapper.readValue(body, GeocodingResponse::class.java)
            val first = response.results?.firstOrNull() ?: return null
            GeocodeResult(first.latitude, first.longitude)
        } catch (e: Exception) {
            log.warn("Failed to geocode '{}': {}", query, e.message)
            null
        }
    }

    fun getWeatherFor(latitude: Double, longitude: Double, dateTime: LocalDateTime): ResolvedWeather? {
        val today = LocalDate.now()
        val daysOut = java.time.temporal.ChronoUnit.DAYS.between(today, dateTime.toLocalDate())

        return try {
            if (daysOut < 0) {
                fetchArchive(latitude, longitude, dateTime)
            } else if (daysOut <= 15) {
                fetchForecast(latitude, longitude, dateTime)
            } else {
                null // too far in the future to have a meaningful forecast
            }
        } catch (e: Exception) {
            log.warn("Failed to fetch weather for ({}, {}) at {}: {}", latitude, longitude, dateTime, e.message)
            null
        }
    }

    private fun fetchForecast(latitude: Double, longitude: Double, dateTime: LocalDateTime): ResolvedWeather? {
        val url = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=$latitude&longitude=$longitude" +
            "&hourly=temperature_2m,relative_humidity_2m" +
            "&start_date=${dateTime.toLocalDate()}&end_date=${dateTime.toLocalDate()}" +
            "&timezone=auto"
        val body = restTemplate.getForObject(url, String::class.java) ?: return null
        val response = objectMapper.readValue(body, OpenMeteoHourlyResponse::class.java)
        return extractHourly(response, dateTime.toLocalTime(), "open-meteo-forecast")
    }

    private fun fetchArchive(latitude: Double, longitude: Double, dateTime: LocalDateTime): ResolvedWeather? {
        val url = "https://archive-api.open-meteo.com/v1/archive" +
            "?latitude=$latitude&longitude=$longitude" +
            "&hourly=temperature_2m,relative_humidity_2m" +
            "&start_date=${dateTime.toLocalDate()}&end_date=${dateTime.toLocalDate()}" +
            "&timezone=auto"
        val body = restTemplate.getForObject(url, String::class.java) ?: return null
        val response = objectMapper.readValue(body, OpenMeteoHourlyResponse::class.java)
        return extractHourly(response, dateTime.toLocalTime(), "open-meteo-archive")
    }

    private fun extractHourly(response: OpenMeteoHourlyResponse, time: LocalTime, source: String): ResolvedWeather? {
        val hourly = response.hourly ?: return null
        val times = hourly.time ?: return null
        val temps = hourly.temperature_2m ?: return null
        val humidity = hourly.relative_humidity_2m ?: return null

        val targetHour = time.hour
        val index = times.indexOfFirst { it.endsWith("T" + targetHour.toString().padStart(2, '0') + ":00") }
        if (index < 0 || index >= temps.size || index >= humidity.size) return null

        return ResolvedWeather(temps[index], humidity[index], source)
    }
}

private data class GeocodingResponse(val results: List<GeocodingResult>?)
private data class GeocodingResult(val latitude: Double, val longitude: Double)

private data class OpenMeteoHourlyResponse(val hourly: HourlyBlock?)
private data class HourlyBlock(
    val time: List<String>?,
    val temperature_2m: List<Double>?,
    val relative_humidity_2m: List<Double>?
)
