package com.terkula.uaxctf.training.repository

import com.terkula.uaxctf.training.model.liveworkout.LiveWorkout
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface LiveWorkoutRepository: CrudRepository<LiveWorkout, String> {

    fun findByWorkoutUuid(workoutUuid: String): List<LiveWorkout>

    fun findFirstByUsernameOrderByTimestampDesc(username: String): LiveWorkout?

}