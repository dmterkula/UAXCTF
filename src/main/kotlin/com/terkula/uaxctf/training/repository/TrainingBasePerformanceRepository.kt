package com.terkula.uaxctf.training.repository

import com.terkula.uaxctf.training.model.trainingbase.TrainingBasePerformance
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.sql.Timestamp

@Repository
interface TrainingBasePerformanceRepository: CrudRepository<TrainingBasePerformance, Int> {

    fun findByRunnerId(runnerId: Int): List<TrainingBasePerformance>

    fun findByRunnerIdAndSeason(runnerId: Int, season: String): List<TrainingBasePerformance>

    fun findByRunnerIdAndEvent(runnerId: Int, event: String): List<TrainingBasePerformance>

    fun findByRunnerIdAndEventAndYear(runnerId: Int, event: String, year: String): List<TrainingBasePerformance>

    fun findByRunnerIdAndSeasonAndYear(runnerId: Int, season: String, year: String): List<TrainingBasePerformance>

    fun findByRunnerIdAndEventAndSeason(runnerId: Int, event: String, season: String): List<TrainingBasePerformance>

    fun findByRunnerIdAndEventAndSeasonAndYear(runnerId: Int, event: String, season: String, year: String): List<TrainingBasePerformance>

    fun findBySeasonAndYear(season: String, year: String): List<TrainingBasePerformance>

    fun findBySeasonAndYearAndCreatedAtLessThanEqualOrderByCreatedAtDesc(season: String, year: String, date: Timestamp, pageable: Pageable): List<TrainingBasePerformance>

    fun findByEventAndSeasonAndYear(event: String, season: String, year: String): List<TrainingBasePerformance>

    fun findByUuid(uuid: String): TrainingBasePerformance?

    // Timestamp-based queries for historical tracking
    fun findByRunnerIdAndSeasonOrderByCreatedAtDesc(runnerId: Int, season: String): List<TrainingBasePerformance>

    fun findByRunnerIdAndEventOrderByCreatedAtDesc(runnerId: Int, event: String): List<TrainingBasePerformance>

    fun findByRunnerIdAndSeasonAndYearOrderByCreatedAtDesc(runnerId: Int, season: String, year: String): List<TrainingBasePerformance>

    fun findByRunnerIdAndEventAndSeasonOrderByCreatedAtDesc(runnerId: Int, event: String, season: String): List<TrainingBasePerformance>

    fun findByRunnerIdAndEventAndSeasonAndYearOrderByCreatedAtDesc(runnerId: Int, event: String, season: String, year: String): List<TrainingBasePerformance>

    fun findByRunnerIdAndSeasonAndCreatedAtLessThanEqualOrderByCreatedAtDesc(runnerId: Int, season: String, date: Timestamp, pageable: Pageable): List<TrainingBasePerformance>

    fun findByRunnerIdAndEventAndSeasonAndCreatedAtLessThanEqualOrderByCreatedAtDesc(runnerId: Int, event: String, season: String, date: Timestamp, pageable: Pageable): List<TrainingBasePerformance>

    fun findByRunnerIdAndEventAndSeasonAndYearAndCreatedAtLessThanEqualOrderByCreatedAtDesc(runnerId: Int, event: String, season: String, year: String, date: Timestamp, pageable: Pageable): List<TrainingBasePerformance>
}