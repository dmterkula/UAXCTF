package com.terkula.uaxctf.training.response

class SplitsResponse(
        val componentUUID: String,
        val runnerId: Int,
        val splits: List<SplitResponse>) {


}

class SplitResponse(val uuid: String, val number: Int, val time: String)