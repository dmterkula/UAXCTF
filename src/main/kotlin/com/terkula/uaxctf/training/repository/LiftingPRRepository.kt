package com.terkula.uaxctf.training.repository

import com.terkula.uaxctf.training.model.lifting.LiftingPR
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface LiftingPRRepository : CrudRepository<LiftingPR, Int> {

    fun findByUuid(uuid: String): LiftingPR?

    fun findByRunnerId(runnerId: Int): List<LiftingPR>

    fun findByRunnerIdAndExerciseUuid(runnerId: Int, exerciseUuid: String): List<LiftingPR>

    fun findByRunnerIdAndExerciseUuidAndPrType(
        runnerId: Int,
        exerciseUuid: String,
        prType: String
    ): LiftingPR?

    fun findByExerciseUuid(exerciseUuid: String): List<LiftingPR>

    fun findByRunnerIdOrderByAchievedDateDesc(runnerId: Int): List<LiftingPR>
}
