package com.terkula.uaxctf.training.request.crosstraining

class CreateCrossTrainingRecordRequest(
        var uuid: String,
        var crossTrainingUuid: String,
        var runnerId: Int,
        var distance: Double,
        var time: String,
        var notes: String?,
        var coachesNotes: String?,
        var effortLevel: Double?,
        var avgHr: Int?,
        var maxHr: Int?,
        var avgPower: Int?,
        var maxPower: Int?,
) {
}