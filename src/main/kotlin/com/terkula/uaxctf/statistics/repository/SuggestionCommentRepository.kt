package com.terkula.uaxctf.statistics.repository

import com.terkula.uaxctf.statisitcs.model.SuggestionComment
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface SuggestionCommentRepository : CrudRepository<SuggestionComment, Int> {
    fun findByUuid(uuid: String): SuggestionComment?
    fun findBySuggestionUuidOrderByCreatedAtAsc(suggestionUuid: String): List<SuggestionComment>
    fun deleteBySuggestionUuid(suggestionUuid: String)
}
