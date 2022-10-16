package com.terkula.uaxctf.training.repository

import com.terkula.uaxctf.training.model.WorkoutComponent
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface WorkoutComponentRepository: CrudRepository<WorkoutComponent, Int> {

    fun findByUuid(uuid: String): List<WorkoutComponent>

    fun findByWorkoutUuid(workoutUUID: String): List<WorkoutComponent>

}