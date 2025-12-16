package com.terkula.uaxctf.training.repository

import com.terkula.uaxctf.training.model.lifting.LiftingPRHistory
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.sql.Timestamp

@Repository
interface LiftingPRHistoryRepository : CrudRepository<LiftingPRHistory, Int> {

    fun findByUuid(uuid: String): LiftingPRHistory?

    fun findByRunnerId(runnerId: Int): List<LiftingPRHistory>

    fun findByRunnerIdAndExerciseUuid(runnerId: Int, exerciseUuid: String): List<LiftingPRHistory>

    fun findByRunnerIdAndExerciseUuidAndPrType(
        runnerId: Int,
        exerciseUuid: String,
        prType: String
    ): List<LiftingPRHistory>

    fun findByRunnerIdAndExerciseUuidAndPrTypeOrderByAchievedDateDesc(
        runnerId: Int,
        exerciseUuid: String,
        prType: String
    ): List<LiftingPRHistory>

    fun findByLiftingRecordUuid(liftingRecordUuid: String): List<LiftingPRHistory>

    fun findByRunnerIdAndAchievedDateBetween(
        runnerId: Int,
        startDate: Timestamp,
        endDate: Timestamp
    ): List<LiftingPRHistory>

    fun findByRunnerIdOrderByAchievedDateDesc(runnerId: Int): List<LiftingPRHistory>

    @Query("""
        SELECT lph FROM LiftingPRHistory lph
        WHERE lph.runnerId = :runnerId
        AND lph.exerciseUuid = :exerciseUuid
        AND lph.prType = :prType
        AND lph.achievedDate <= :achievedDate
        ORDER BY lph.achievedDate DESC
    """)
    fun findHistoricalPRs(
        @Param("runnerId") runnerId: Int,
        @Param("exerciseUuid") exerciseUuid: String,
        @Param("prType") prType: String,
        @Param("achievedDate") achievedDate: Timestamp
    ): List<LiftingPRHistory>
}
