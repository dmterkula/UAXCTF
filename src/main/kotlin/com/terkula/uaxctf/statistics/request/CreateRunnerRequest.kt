package com.terkula.uaxctf.statistics.request

class CreateRunnerRequest(
    val name: String,
    val graduatingClass: String,
    var isActive: Boolean = true,
    var doesXc: Boolean = true,
    var doesTrack: Boolean = true,
    var deviceId: String?,
    var team: String
    )