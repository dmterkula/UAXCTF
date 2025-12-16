package com.terkula.uaxctf.training.repository

import com.terkula.uaxctf.training.model.lifting.LiftingActivity
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.sql.Date

@Repository
interface LiftingActivityRepository : CrudRepository<LiftingActivity, Int> {

    fun findByUuid(uuid: String): LiftingActivity?

    fun findByDateBetween(startDate: Date, endDate: Date): List<LiftingActivity>

    fun findByDateBetweenAndTeam(startDate: Date, endDate: Date, team: String): List<LiftingActivity>

    fun findByDateBetweenAndSeasonAndTeam(
        startDate: Date,
        endDate: Date,
        season: String,
        team: String
    ): List<LiftingActivity>

    fun findByTeamOrderByDateDesc(team: String): List<LiftingActivity>
}
