package com.terkula.uaxctf.statistics.repository

import com.terkula.uaxctf.statisitcs.model.CoachingGroup
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CoachingGroupRepository : CrudRepository<CoachingGroup, Long> {
    fun findByYear(year: String): List<CoachingGroup>
}
