package com.terkula.uaxctf.training.request

class LogWorkoutResultsRequest(
    val runnerId: Int,
    val workoutUuid: String,
    val totalDistance: Double,
    val componentsSplits: List<ComponentsSplitsRequest>
)


class ComponentsSplitsRequest (
    val componentUUID: String,
    val splits: List<Split>
)