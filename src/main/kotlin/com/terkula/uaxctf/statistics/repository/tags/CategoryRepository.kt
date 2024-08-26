package com.terkula.uaxctf.statistics.repository.tags

import com.terkula.uaxctf.training.model.tags.Category
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CategoryRepository : CrudRepository<Category, Int> {

    fun findByRunnerId(runnerId: Int): List<Category>

    fun findByRunnerIdOrRunnerIdIsNull(runnerId: Int): List<Category>

    fun findByUuid(uuid: String): List<Category>

    fun deleteByUuid(uuid: String): List<Category>

}