package com.terkula.uaxctf.training.repository

import com.terkula.uaxctf.training.model.WorkoutSplitV2
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface WorkoutSplitV2Repository: CrudRepository<WorkoutSplitV2, Int> {

    fun findByComponentUUIDAndRunnerId(uuid: String, runnerId: Int): List<WorkoutSplitV2>

    @Transactional
    fun deleteByUuid(uuid: String): List<WorkoutSplitV2>

}