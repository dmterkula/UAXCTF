package com.terkula.uaxctf.training.repository

import com.terkula.uaxctf.training.model.RunnersTrainingRun
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.Date

@Repository
interface RunnersTrainingRunRepository: CrudRepository<RunnersTrainingRun, Int> {

    fun findByTrainingRunUuid(uuid: String): List<RunnersTrainingRun>

    fun findByRunnerId(runnerId: Int): List<RunnersTrainingRun>

    fun findByUuid(uuid: String): List<RunnersTrainingRun>

    fun findByTrainingRunUuidAndRunnerId(trainingRunUuid: String, runnerId: Int): List<RunnersTrainingRun>

    @Transactional
    fun deleteByUuid(uuid: String): List<RunnersTrainingRun>

}