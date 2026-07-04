package com.terkula.uaxctf.statistics.repository

import com.terkula.uaxctf.statisitcs.model.EncouragementReaction
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface EncouragementReactionRepository : CrudRepository<EncouragementReaction, Int> {
    fun findByEncouragementUuid(encouragementUuid: String): List<EncouragementReaction>
    fun findByUuid(uuid: String): EncouragementReaction?

    @Transactional
    fun deleteByUuid(uuid: String)

    @Transactional
    fun deleteByEncouragementUuid(encouragementUuid: String)
}
