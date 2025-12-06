package com.terkula.uaxctf.statistics.repository

import com.terkula.uaxctf.statisitcs.model.PointTransaction
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PointTransactionRepository : CrudRepository<PointTransaction, Long> {
    fun findByRunnerIdOrderByCreatedAtDesc(runnerId: Int): List<PointTransaction>
    fun findByTransactionType(transactionType: String): List<PointTransaction>
    fun findByActivityUuid(activityUuid: String): PointTransaction?
    fun findByRunnerIdAndSeasonOrderByCreatedAtDesc(runnerId: Int, season: String): List<PointTransaction>
    fun findTop50ByRunnerIdOrderByCreatedAtDesc(runnerId: Int): List<PointTransaction>
}