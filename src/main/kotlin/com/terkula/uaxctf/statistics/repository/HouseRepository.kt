package com.terkula.uaxctf.statistics.repository

import com.terkula.uaxctf.statisitcs.model.House
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface HouseRepository : CrudRepository<House, Long> {
    fun findBySeasonAndYear(season: String, year: String): List<House>
}
