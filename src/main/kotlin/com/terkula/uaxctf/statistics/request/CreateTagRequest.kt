package com.terkula.uaxctf.statistics.request

class CreateTagRequest(
        val runnerId: Int?,
        val tag: String,
        val category: String,
        val uuid: String
) {
}