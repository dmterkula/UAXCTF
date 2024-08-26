package com.terkula.uaxctf.statistics.repository.tags

import com.terkula.uaxctf.training.model.tags.Tag
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface TagRepository: CrudRepository<Tag, Int> {

    fun findByRunnerId(runnerId: Int): List<Tag>

    fun findByRunnerIdOrRunnerIdIsNull(runnerId: Int): List<Tag>

    fun findByUuid(uuid: String): List<Tag>

    fun deleteByUuid(uuid: String): List<Tag>

    fun deleteByRunnerIdAndCategory(runnerId: Int?, category: String): List<Tag>

}