package com.terkula.uaxctf.training.request

class CreateBaseTrainingPerformanceRequest(
        val runnerId: Int,
        var fractionOfMiles: Double,
        var seconds: Int,
        var season: String,
        var event: String,
        var year: String,
        var uuid: String
) {
}