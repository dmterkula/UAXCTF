package com.terkula.uaxctf.training.repository

import com.terkula.uaxctf.training.model.TrainingRun
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.sql.Date

@Repository
interface TrainingRunRepository: CrudRepository<TrainingRun, Int> {

    fun findByDate(date: Date): List<TrainingRun>

    fun findByUuid(uuid: String): List<TrainingRun>

    fun findByDateBetween(start: Date, end: Date): List<TrainingRun>

}