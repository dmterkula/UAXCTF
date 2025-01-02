package com.terkula.uaxctf.training.repository

import com.terkula.uaxctf.training.model.CrossTraining.CrossTrainingRecord
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface CrossTrainingRecordRepository: CrudRepository<CrossTrainingRecord, Int> {

    fun findByCrossTrainingUuid(uuid: String): List<CrossTrainingRecord>

    fun findByRunnerId(runnerId: Int): List<CrossTrainingRecord>

    fun findByUuid(uuid: String): List<CrossTrainingRecord>

    fun findByCrossTrainingUuidAndRunnerId(crossTrainingUuid: String, runnerId: Int): List<CrossTrainingRecord>

    @Transactional
    fun deleteByUuid(uuid: String): List<CrossTrainingRecord>

}