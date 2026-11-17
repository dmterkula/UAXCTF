package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statisitcs.model.Venue
import com.terkula.uaxctf.statistics.service.VenueService
import org.springframework.web.bind.annotation.*

data class CreateVenueRequest(val name: String, val imagePath: String?, val team: String, val address: String? = null, val isIndoor: Boolean = false)
data class UpdateVenueRequest(val name: String?, val imagePath: String?, val address: String? = null, val isIndoor: Boolean? = null)

@RestController
class VenueController(val venueService: VenueService) {

    @RequestMapping(value = ["/venues"], method = [RequestMethod.GET])
    fun getVenues(
        @RequestParam(value = "team", required = true) team: String
    ): List<Venue> = venueService.getVenuesForTeam(team)

    @RequestMapping(value = ["/venue/create"], method = [RequestMethod.POST])
    fun createVenue(@RequestBody request: CreateVenueRequest): Venue =
        venueService.createVenue(request.name, request.imagePath, request.team, request.address, request.isIndoor)

    @RequestMapping(value = ["/venue/update/{id}"], method = [RequestMethod.PUT])
    fun updateVenue(
        @PathVariable id: Int,
        @RequestBody request: UpdateVenueRequest
    ): Venue? = venueService.updateVenue(id, request.name, request.imagePath, request.address, request.isIndoor)

    @RequestMapping(value = ["/venue/delete/{id}"], method = [RequestMethod.DELETE])
    fun deleteVenue(@PathVariable id: Int) = venueService.deleteVenue(id)
}
