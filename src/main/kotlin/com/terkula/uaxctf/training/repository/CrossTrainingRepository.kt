package com.terkula.uaxctf.training.repository

import com.terkula.uaxctf.training.model.CrossTraining.CrossTraining
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.sql.Date

@Repository
interface CrossTrainingRepository: CrudRepository<CrossTraining, Int> {

    fun findByDate(date: Date): List<CrossTraining>

    fun findByUuid(uuid: String): List<CrossTraining>

    fun findByDateBetween(start: Date, end: Date): List<CrossTraining>

    fun findByDateBetweenAndSeason(start: Date, end: Date, season: String): List<CrossTraining>

    fun findByDateBetweenAndSeasonAndTeam(start: Date, end: Date, season: String, team: String): List<CrossTraining>

}