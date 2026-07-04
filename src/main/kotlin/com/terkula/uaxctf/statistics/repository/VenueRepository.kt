package com.terkula.uaxctf.statistics.repository

import com.terkula.uaxctf.statisitcs.model.Venue
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface VenueRepository : CrudRepository<Venue, Int> {
    fun findByTeam(team: String): List<Venue>
    fun findByNameContainsIgnoreCaseAndTeam(name: String, team: String): List<Venue>
}
