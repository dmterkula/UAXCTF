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
    fun findByEventAndSeason(event: String, season: String): List<BaseTrainingPercentage>

    fun findBySeasonAndTypeAndPaceName(season: String, type: String, paceName: String): List<BaseTrainingPercentage>
    fun findByEventAndSeasonAndTypeAndPaceNameAndPercent(event: String, season: String, type: String, paceName: String, percent: Int): List<BaseTrainingPercentage>

}