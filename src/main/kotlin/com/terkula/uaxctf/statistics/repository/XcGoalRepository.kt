package com.terkula.uaxctf.statistics.repository

import com.terkula.uaxctf.statisitcs.model.XcGoal
import org.springframework.data.repository.CrudRepository

interface XcGoalRepository: CrudRepository<XcGoal, Int> {

    fun findBySeason(season: String): List<XcGoal>

    fun findByRunnerId(runnerId: Int): List<XcGoal>

    fun findByRunnerIdAndSeasonAndTypeAndValue(runnerId: Int, season: String, type: String, value: String): List<XcGoal>

    fun findBySeasonAndTrackGoal(season: String, trackGoal: Boolean): List<XcGoal>

    fun findBySeasonAndEvent(season: String, event: String): List<XcGoal>

}