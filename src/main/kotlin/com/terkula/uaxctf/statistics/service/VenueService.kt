package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statisitcs.model.Venue
import com.terkula.uaxctf.statistics.repository.VenueRepository
import com.terkula.uaxctf.weather.WeatherService
import org.springframework.stereotype.Service

@Service
class VenueService(val venueRepository: VenueRepository, val weatherService: WeatherService) {

    fun getVenuesForTeam(team: String): List<Venue> =
        venueRepository.findByTeam(team)

    fun getVenueById(id: Int): Venue? =
        venueRepository.findById(id).orElse(null)

    fun createVenue(name: String, imagePath: String?, team: String, address: String? = null, isIndoor: Boolean = false): Venue {
        val venue = Venue(name, imagePath, team, address, isIndoor = isIndoor)
        geocodeIfNeeded(venue)
        return venueRepository.save(venue)
    }

    fun updateVenue(id: Int, name: String?, imagePath: String?, address: String? = null, isIndoor: Boolean? = null): Venue? {
        val venue = venueRepository.findById(id).orElse(null) ?: return null
        name?.let { venue.name = it }
        imagePath?.let { venue.imagePath = it }
        isIndoor?.let { venue.isIndoor = it }
        if (address != null && address != venue.address) {
            venue.address = address
            venue.latitude = null
            venue.longitude = null
        }
        // Always retry geocoding if we still don't have coordinates — e.g. the address was
        // unresolvable the first time (typo, too specific) and the coach re-saved it unchanged.
        geocodeIfNeeded(venue)
        return venueRepository.save(venue)
    }

    /** Resolves lat/lon from the venue's address via Open-Meteo geocoding, if not already set. */
    private fun geocodeIfNeeded(venue: Venue) {
        if (venue.latitude != null && venue.longitude != null) return
        val address = venue.address ?: return
        val geocoded = weatherService.geocode(address) ?: return
        venue.latitude = geocoded.latitude
        venue.longitude = geocoded.longitude
    }

    fun deleteVenue(id: Int) = venueRepository.deleteById(id)
}
