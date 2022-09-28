package com.terkula.uaxctf.training.repository

import com.terkula.uaxctf.training.model.Workout
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.sql.Date

@Repository
interface WorkoutRepository: CrudRepository<Workout, Int> {

    fun findByDate(date: Date): List<Workout>

    fun findByUuid(uuid: String): List<Workout>

    fun findByDateAndTitle(date: Date, title: String): List<Workout>

    fun findByDateBetween(start: Date, end: Date): List<Workout>

}