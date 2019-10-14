package com.terkula.uaxctf.statistics.repository

import com.terkula.uaxctf.statisitcs.model.TimeTrial
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface TimeTrialRepository: CrudRepository<TimeTrial, Int> {

    fun findByRunnerIdAndSeason(runnerId: Int, season: String): List<TimeTrial>

}