package com.terkula.uaxctf.statistics.repository

import com.terkula.uaxctf.statisitcs.model.EncouragementSave
import com.terkula.uaxctf.statisitcs.model.EncouragementSaveId
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface EncouragementSaveRepository : CrudRepository<EncouragementSave, EncouragementSaveId> {
    fun findByRunnerId(runnerId: Int): List<EncouragementSave>
    fun findByEncouragementUuidAndRunnerId(encouragementUuid: String, runnerId: Int): EncouragementSave?

    @Transactional
    fun deleteByEncouragementUuidAndRunnerId(encouragementUuid: String, runnerId: Int)

    @Transactional
    fun deleteByEncouragementUuid(encouragementUuid: String)
}
