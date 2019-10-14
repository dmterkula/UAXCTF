package com.terkula.uaxctf.training.repository

import com.terkula.uaxctf.training.model.RawWorkout
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface RawWorkoutRepository: CrudRepository<RawWorkout, String> {
}