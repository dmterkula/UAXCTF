package com.terkula.uaxctf.statistics.repository

import com.terkula.uaxctf.statisitcs.model.VenueCourseProfile
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface VenueCourseProfileRepository : CrudRepository<VenueCourseProfile, Int> {
    fun findByVenueId(venueId: Int): List<VenueCourseProfile>
    fun findByVenueIdAndIsCurrentTrue(venueId: Int): VenueCourseProfile?
}
