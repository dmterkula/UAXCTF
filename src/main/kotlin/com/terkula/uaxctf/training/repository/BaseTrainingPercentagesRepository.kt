package com.terkula.uaxctf.training.repository

import com.terkula.uaxctf.training.model.trainingbase.BaseTrainingPercentage
import com.terkula.uaxctf.training.model.trainingbase.TrainingBasePerformance
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface BaseTrainingPercentagesRepository: CrudRepository<BaseTrainingPercentage, Int> {

    fun findByEventAndType(event: String, type: String): List<BaseTrainingPercentage>

    fun findByType(type: String): List<BaseTrainingPercentage>

    fun findByTypeAndSeason(type: String, season: String): List<BaseTrainingPercentage>

}