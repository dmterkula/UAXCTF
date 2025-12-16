package com.terkula.uaxctf.training.repository

import com.terkula.uaxctf.training.model.lifting.Exercise
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface ExerciseRepository : CrudRepository<Exercise, Int> {

    fun findByUuid(uuid: String): Exercise?

    fun findByIsGlobalTrue(): List<Exercise>

    fun findByTeamAndIsGlobalFalse(team: String): List<Exercise>

    fun findByIsGlobalTrueOrTeam(team: String?): List<Exercise>

    fun findByCategory(category: String): List<Exercise>

    fun findByName(name: String): List<Exercise>
}
