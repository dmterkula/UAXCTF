package com.terkula.uaxctf.statistics.repository

import com.terkula.uaxctf.statisitcs.model.SuggestionThumbsUp
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface SuggestionThumbsUpRepository : CrudRepository<SuggestionThumbsUp, Int> {
    fun findBySuggestionUuid(suggestionUuid: String): List<SuggestionThumbsUp>
    fun findBySuggestionUuidAndRunnerId(suggestionUuid: String, runnerId: Int): SuggestionThumbsUp?
    fun countBySuggestionUuid(suggestionUuid: String): Long
    fun deleteBySuggestionUuidAndRunnerId(suggestionUuid: String, runnerId: Int)
    fun deleteBySuggestionUuid(suggestionUuid: String)
}
