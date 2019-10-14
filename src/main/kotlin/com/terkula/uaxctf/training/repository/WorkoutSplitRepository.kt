package com.terkula.uaxctf.training.repository

import com.terkula.uaxctf.training.model.WorkoutSplit
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface WorkoutSplitRepository: CrudRepository<WorkoutSplit, Int> {

    fun findByWorkoutId(workoutId: Int): List<WorkoutSplit>

    fun findByWorkoutIdAndRunnerId(id: Int, runnerId: Int): List<WorkoutSplit>

}