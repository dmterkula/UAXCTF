package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statisitcs.model.Venue
import com.terkula.uaxctf.statistics.repository.VenueRepository
import org.springframework.stereotype.Service

@Service
class VenueService(val venueRepository: VenueRepository) {

    fun getVenuesForTeam(team: String): List<Venue> =
        venueRepository.findByTeam(team)

    fun getVenueById(id: Int): Venue? =
        venueRepository.findById(id).orElse(null)

    fun createVenue(name: String, imagePath: String?, team: String): Venue =
        venueRepository.save(Venue(name, imagePath, team))

    fun updateVenue(id: Int, name: String?, imagePath: String?): Venue? {
        val venue = venueRepository.findById(id).orElse(null) ?: return null
        name?.let { venue.name = it }
        imagePath?.let { venue.imagePath = it }
        return venueRepository.save(venue)
    }

    fun deleteVenue(id: Int) = venueRepository.deleteById(id)
}
