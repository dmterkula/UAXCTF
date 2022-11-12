package com.terkula.uaxctf.training.request

class CreateSplitsRequest (
    val componentUUID: String,
    val runnerId: Int,
    val splits: List<Split>
)

class Split(
    val uuid: String,
    val number: Int,
    val value: String
)