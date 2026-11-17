package com.terkula.uaxctf.statistics.repository

import com.terkula.uaxctf.statisitcs.model.CoachReplyDismissal
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.Date

@Repository
interface CoachReplyDismissalRepository : CrudRepository<CoachReplyDismissal, Int> {

    fun findByCoachUsername(coachUsername: String): List<CoachReplyDismissal>

    fun findByCoachUsernameAndEntityUuid(coachUsername: String, entityUuid: String): CoachReplyDismissal?

    @Transactional
    fun deleteByCoachUsernameAndEntityUuid(coachUsername: String, entityUuid: String): Int

    /** Backs "clear everything older than X" -- the bulk escape hatch for a neglected queue. */
    fun findByCoachUsernameAndLogDateLessThan(coachUsername: String, logDate: Date): List<CoachReplyDismissal>
}
