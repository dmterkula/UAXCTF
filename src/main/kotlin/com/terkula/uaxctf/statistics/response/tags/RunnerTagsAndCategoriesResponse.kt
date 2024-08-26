package com.terkula.uaxctf.statistics.response.tags

import com.terkula.uaxctf.training.model.tags.Category
import com.terkula.uaxctf.training.model.tags.Tag

class RunnerTagsAndCategoriesResponse(
        val tags: List<Tag>,
        val categories: List<Category>
) {
}