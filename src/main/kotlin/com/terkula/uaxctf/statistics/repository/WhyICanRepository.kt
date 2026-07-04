package com.terkula.uaxctf.statistics.repository

import com.terkula.uaxctf.statisitcs.model.WhyICan
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface WhyICanRepository : CrudRepository<WhyICan, Int> {
    fun findByUuid(uuid: String): WhyICan?
    fun findByRunnerIdOrderByCreatedAtDesc(runnerId: Int): List<WhyICan>
    fun deleteByUuid(uuid: String)
}
