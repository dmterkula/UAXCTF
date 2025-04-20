package com.terkula.uaxctf.training.repository

import com.terkula.uaxctf.training.model.coachdavidisms.CoachDavidIsm
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CoachDavidIsmsRepository: CrudRepository<CoachDavidIsm, Int> {

    fun findByForUa(forUa: Boolean): List<CoachDavidIsm>

    fun findByForNu(forNu: Boolean): List<CoachDavidIsm>

}