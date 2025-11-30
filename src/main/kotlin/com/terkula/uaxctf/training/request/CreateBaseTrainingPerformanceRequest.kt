package com.terkula.uaxctf.training.request

/**
 * Request to create a new base training performance record.
 *
 * Note: This always creates a NEW record to preserve historical tracking.
 * The forceCreate parameter is kept for API backward compatibility but does not affect behavior.
 */
class CreateBaseTrainingPerformanceRequest(
    val runnerId: Int,
    var fractionOfMiles: Double,
    var seconds: Int,
    var season: String,
    var event: String,
    var year: String,
    var uuid: String,
    var forceCreate: Boolean = true  // Kept for backward compatibility, always creates new records
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