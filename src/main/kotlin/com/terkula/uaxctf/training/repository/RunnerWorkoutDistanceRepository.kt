package com.terkula.uaxctf.training.repository

import com.terkula.uaxctf.training.model.RunnerWorkoutDistance
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface RunnerWorkoutDistanceRepository: CrudRepository<RunnerWorkoutDistance, Int> {

    fun findByWorkoutUuid(workoutUuid: String): List<RunnerWorkoutDistance>

    fun findByWorkoutUuidAndRunnerId(workoutUuid: String, runnerId: Int): List<RunnerWorkoutDistance>

}