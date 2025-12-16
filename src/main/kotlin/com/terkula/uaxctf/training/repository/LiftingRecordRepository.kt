package com.terkula.uaxctf.training.repository

import com.terkula.uaxctf.training.model.lifting.LiftingRecord
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.Date

@Repository
interface LiftingRecordRepository : CrudRepository<LiftingRecord, Int> {

    fun findByUuid(uuid: String): LiftingRecord?

    fun findByLiftingActivityUuid(activityUuid: String): List<LiftingRecord>

    fun findByLiftingActivityUuidAndRunnerId(activityUuid: String, runnerId: Int): LiftingRecord?

    fun findByRunnerIdOrderByDateLoggedDesc(runnerId: Int): List<LiftingRecord>

    fun findByRunnerId(runnerId: Int): List<LiftingRecord>

    fun findByRunnerIdAndDateLoggedBetween(
        runnerId: Int,
        startDate: java.sql.Timestamp,
        endDate: java.sql.Timestamp
    ): List<LiftingRecord>

    @Query("""
        SELECT lr FROM LiftingRecord lr
        WHERE lr.runnerId = :runnerId
        AND lr.liftingActivityUuid IN (
            SELECT la.uuid FROM LiftingActivity la
            WHERE la.date BETWEEN :startDate AND :endDate
        )
    """)
    fun findByRunnerIdAndActivityDateBetween(
        @Param("runnerId") runnerId: Int,
        @Param("startDate") startDate: Date,
        @Param("endDate") endDate: Date
    ): List<LiftingRecord>

    @Transactional
    fun deleteByUuid(uuid: String): Int
}
