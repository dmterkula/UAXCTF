package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statistics.request.CreateCategoryRequest
import com.terkula.uaxctf.statistics.request.CreateTagRequest
import com.terkula.uaxctf.statistics.response.tags.RunnerTagsAndCategoriesResponse
import com.terkula.uaxctf.statistics.service.TagsAndCategoriesService
import com.terkula.uaxctf.training.model.tags.Category
import com.terkula.uaxctf.training.model.tags.Tag
import io.swagger.annotations.ApiOperation
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@RestController
@Validated
class TagsAndCategoriesController (
       val tagsAndCategoriesService: TagsAndCategoriesService
) {

    @ApiOperation("get journal entry")
    @RequestMapping(value = ["tagsAndCategories/forRunner/get"], method = [RequestMethod.GET])
    fun getDailyJournalEntry(
            @RequestParam(name = "runnerId") runnerId: Int,
    ): RunnerTagsAndCategoriesResponse {

        return tagsAndCategoriesService.getTagsAndCategoriesForRunner(runnerId)
    }

    @ApiOperation("get Tag by uuid")
    @RequestMapping(value = ["tagsAndCategories/tag/get"], method = [RequestMethod.GET])
    fun getTag(
            @RequestParam(name = "uuid") uuid: String
    ): Tag? {

        return tagsAndCategoriesService.getTag(uuid)
    }

    @ApiOperation("get Tag by uuid")
    @RequestMapping(value = ["tagsAndCategories/category/get"], method = [RequestMethod.GET])
    fun getCategory(
            @RequestParam(name = "uuid") uuid: String
    ): Category? {

        return tagsAndCategoriesService.getCategory(uuid)
    }

    @ApiOperation("create or update journal entry")
    @RequestMapping(value = ["tagsAndCategories/tags/create"], method = [RequestMethod.POST])
    fun createTag(
            @RequestBody @Valid createTagRequest: CreateTagRequest
    ): Tag {

        return tagsAndCategoriesService.createTag(createTagRequest)
    }

    @ApiOperation("create or update journal entry")
    @RequestMapping(value = ["tagsAndCategories/categories/create"], method = [RequestMethod.POST])
    fun createCategory(
            @RequestBody @Valid createCategoryRequest: CreateCategoryRequest
    ): Category {

        return tagsAndCategoriesService.createCategory(createCategoryRequest)
    }

    @ApiOperation("Delete Tag")
    @RequestMapping(value = ["tagsAndCategories/tags/delete"], method = [RequestMethod.DELETE])
    fun deleteTag(
            @RequestBody @Valid deleteTagRequest: CreateTagRequest
    ): Tag {

        return tagsAndCategoriesService.deleteTag(deleteTagRequest)
    }

    @ApiOperation("Delete Tag")
    @RequestMapping(value = ["tagsAndCategories/categories/delete"], method = [RequestMethod.DELETE])
    fun deleteCategory(
            @RequestBody @Valid deleteCategoryRequest: CreateCategoryRequest
    ): Category {

        return tagsAndCategoriesService.deleteCategory(deleteCategoryRequest)
    }


}