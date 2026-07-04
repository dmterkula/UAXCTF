package com.terkula.uaxctf.statistics.repository

import com.terkula.uaxctf.statisitcs.model.WhyICanActivityLink
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface WhyICanActivityLinkRepository : JpaRepository<WhyICanActivityLink, Int> {

    fun findByRunnerIdOrderByDisplayOrderAsc(runnerId: Int): List<WhyICanActivityLink>

    fun findByUuid(uuid: String): WhyICanActivityLink?

    fun findByRunnerIdAndActivityTypeAndActivityUuid(
        runnerId: Int,
        activityType: String,
        activityUuid: String
    ): WhyICanActivityLink?

    @Modifying
    @Transactional
    @Query("DELETE FROM WhyICanActivityLink w WHERE w.uuid = :uuid")
    fun deleteByUuid(uuid: String)
}
