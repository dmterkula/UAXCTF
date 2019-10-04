package com.terkula.uaxctf.statistics.repository

import com.terkula.uaxctf.statisitcs.model.XcGoal
import org.springframework.data.repository.CrudRepository

interface XcGoalRepository: CrudRepository<XcGoal, Int> {

    fun findBySeason(season: String): List<XcGoal>

    fun findByRunnerId(runnerId: Int): List<XcGoal>

}