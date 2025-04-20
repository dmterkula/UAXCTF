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

class CreateRunnerBaseTrainingPercentageRequest(
    val runnerId: Int,
    val event: String,
    val season: String,
    val year: String,
    val pacePercentages: List<PacePercentages>
) {
}

class PacePercentages(
    val percent: Int,
    val paceType: String,
    val paceName: String,
    val uuid: String
)