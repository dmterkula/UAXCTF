package com.terkula.uaxctf.statistics.repository

import com.terkula.uaxctf.statisitcs.model.HouseMembership
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface HouseMembershipRepository : CrudRepository<HouseMembership, Long> {
    fun findByHouseId(houseId: Long): List<HouseMembership>
    fun findByHouseIdIn(houseIds: List<Long>): List<HouseMembership>
    fun findByRunnerIdAndHouseIdIn(runnerId: Int, houseIds: List<Long>): List<HouseMembership>
    fun deleteByHouseIdAndRunnerId(houseId: Long, runnerId: Int)
    fun deleteByHouseId(houseId: Long)
}
