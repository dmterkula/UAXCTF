package com.terkula.uaxctf.training.request

class LogWorkoutResultsRequest(
    val runnerId: Int,
    val workoutUuid: String,
    val totalDistance: Double,
    val notes: String?,
    var time: String,
    var pace: String,
    var warmUpTime: String?,
    var warmUpDistance: Double?,
    var warmUpPace: String?,
    var coolDownTime: String?,
    var coolDownDistance: Double?,
    var coolDownPace: String?,
    val componentsSplits: List<ComponentsSplitsRequest>,
    val coachNotes: String?,
    val painLevel: Double?,
    val painNotes: String?,
    val avgHr: Int?,
    val maxHr: Int?,
    val season: String?,
    val year: String?
)


class ComponentsSplitsRequest (
    val componentUUID: String,
    val splits: List<Split>
)