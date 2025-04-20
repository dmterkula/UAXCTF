package com.terkula.uaxctf.training.repository

import com.terkula.uaxctf.training.model.trainingbase.RunnerBaseTrainingPercentage
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface RunnerTrainingBasePercentageRepository: CrudRepository<RunnerBaseTrainingPercentage, Int> {

    fun findBySeasonAndYear(season: String, year: String): List<RunnerBaseTrainingPercentage>

    fun findBySeasonAndYearAndEvent(season: String, year: String, event: String): List<RunnerBaseTrainingPercentage>

    fun findByRunnerIdAndSeasonAndYear(runnerId: Int, season: String, year: String): List<RunnerBaseTrainingPercentage>

    fun findByUuid(uuid: String): RunnerBaseTrainingPercentage?


}