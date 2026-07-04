package com.terkula.uaxctf.statistics.repository

import com.terkula.uaxctf.statisitcs.model.Encouragement
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.sql.Timestamp

@Repository
interface EncouragementRepository : CrudRepository<Encouragement, Int> {
    fun findByUuid(uuid: String): Encouragement?
    fun findByTeamAndExpiresAtGreaterThanOrderByCreatedAtDesc(team: String, now: Timestamp): List<Encouragement>
    fun findByRecipientRunnerIdAndExpiresAtGreaterThanOrderByCreatedAtDesc(recipientRunnerId: Int, now: Timestamp): List<Encouragement>
    fun findByRecipientRunnerIdIsNullAndExpiresAtGreaterThanOrderByCreatedAtDesc(now: Timestamp): List<Encouragement>
    fun deleteByUuid(uuid: String)
}
