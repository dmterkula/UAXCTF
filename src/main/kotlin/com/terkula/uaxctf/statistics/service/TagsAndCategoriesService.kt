package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statistics.repository.DailyJournalRepository
import com.terkula.uaxctf.statistics.repository.tags.CategoryRepository
import com.terkula.uaxctf.statistics.repository.tags.JournalTagRelationshipRepository
import com.terkula.uaxctf.statistics.repository.tags.TagRepository
import com.terkula.uaxctf.statistics.request.CreateCategoryRequest
import com.terkula.uaxctf.statistics.request.CreateTagRequest
import com.terkula.uaxctf.statistics.response.tags.RunnerTagsAndCategoriesResponse
import com.terkula.uaxctf.training.model.tags.Category
import com.terkula.uaxctf.training.model.tags.Tag
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class TagsAndCategoriesService(
        val tagRepository: TagRepository,
        val categoryRepository: CategoryRepository,
        val journalTagRelationshipRepository: JournalTagRelationshipRepository,
        val dailyJournalRepository: DailyJournalRepository
) {

    fun getTag(uuid: String): Tag? {
        return tagRepository.findByUuid(uuid).firstOrNull()
    }

    fun getCategory(uuid: String): Category? {
        return categoryRepository.findByUuid(uuid).firstOrNull()
    }

    fun getTagsAndCategoriesForRunner(runnerId: Int): RunnerTagsAndCategoriesResponse {
        val tags = tagRepository.findByRunnerIdOrRunnerIdIsNull(runnerId)
        val categories = categoryRepository.findByRunnerIdOrRunnerIdIsNull(runnerId)

        return RunnerTagsAndCategoriesResponse(tags, categories)
    }

    fun createTag(createTagRequest: CreateTagRequest): Tag {

        val existingTag = tagRepository.findByUuid(createTagRequest.uuid).firstOrNull()

        if (existingTag == null) {
            val tag = Tag(createTagRequest.uuid, createTagRequest.tag, createTagRequest.category, createTagRequest.runnerId)
            tagRepository.save(
                    tag
            )
            return tag
        } else {
            existingTag.category = createTagRequest.category
            existingTag.tag = createTagRequest.tag
            existingTag.runnerId = createTagRequest.runnerId

            tagRepository.save(
                    existingTag
            )
            return existingTag
        }
    }

    fun deleteTag(deleteTagRequest: CreateTagRequest): Tag {
        journalTagRelationshipRepository.deleteByRunnerIdAndTagUuid(runnerId = deleteTagRequest.runnerId, tagUuid = deleteTagRequest.uuid)
        return tagRepository.deleteByUuid(deleteTagRequest.uuid).first()!!
    }


    fun deleteCategory(deleteCategory: CreateCategoryRequest): Category {
        val tags = tagRepository.deleteByRunnerIdAndCategory(deleteCategory.runnerId, deleteCategory.category)
        tags.forEach {
            journalTagRelationshipRepository.deleteByRunnerIdAndTagUuid(deleteCategory.runnerId, it.uuid)
        }

        return categoryRepository.deleteByUuid(deleteCategory.uuid).first()
    }



    fun createCategory(createCategoryRequest: CreateCategoryRequest): Category {

        val existingCategory = categoryRepository.findByUuid(createCategoryRequest.uuid).firstOrNull()

        if (existingCategory == null) {
            val category = Category(createCategoryRequest.uuid, createCategoryRequest.category, createCategoryRequest.runnerId)
            categoryRepository.save(
                    category
            )
            return category

        } else {
            existingCategory.name = createCategoryRequest.category
            existingCategory.runnerId = createCategoryRequest.runnerId
            categoryRepository.save(
                    existingCategory
            )
            return existingCategory

        }

    }

}