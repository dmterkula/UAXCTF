package com.terkula.uaxctf.statistics.repository

import com.terkula.uaxctf.statisitcs.model.WhyICanComment
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface WhyICanCommentRepository : CrudRepository<WhyICanComment, Int> {
    fun findByUuid(uuid: String): WhyICanComment?
    fun findByWhyICanRunnerIdOrderByCreatedAtAsc(whyICanRunnerId: Int): List<WhyICanComment>
    fun findByWhyICanRunnerIdAndParentCommentUuidIsNullOrderByCreatedAtAsc(whyICanRunnerId: Int): List<WhyICanComment>
    fun findByParentCommentUuidOrderByCreatedAtAsc(parentCommentUuid: String): List<WhyICanComment>
}
