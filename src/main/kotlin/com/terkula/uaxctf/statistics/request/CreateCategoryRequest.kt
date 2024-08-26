package com.terkula.uaxctf.statistics.request

class CreateCategoryRequest(
        val runnerId: Int?,
        val category: String,
        val uuid: String
) {
}