package com.terkula.uaxctf.statistics.repository

import com.terkula.uaxctf.statisitcs.model.Runner
import org.springframework.data.repository.CrudRepository

interface RunnerRepository : CrudRepository<Runner, Int> {

    fun findByName(name: String): Runner

    fun findByNameContaining(partial: String): MutableList<Runner>

    fun findByGraduatingClassGreaterThan(gradClass: String): MutableList<Runner>

    fun findByGraduatingClassGreaterThanEqual(gradClass: String): MutableList<Runner>

    fun findByGraduatingClass(gradClass: String): MutableList<Runner>

    fun findByDoesTrack(track: Boolean): MutableList<Runner>

    fun findByDoesXc(xc: Boolean): MutableList<Runner>

}
