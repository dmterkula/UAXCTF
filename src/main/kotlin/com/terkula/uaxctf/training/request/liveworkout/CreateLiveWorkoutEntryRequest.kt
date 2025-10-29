package com.terkula.uaxctf.training.request.liveworkout

class CreateLiveWorkoutEntryRequest(
        var workoutUuid: String,
        var username: String,
        var heartRate: Double,
        var avgHeartRate: Double,
        var activeEnergy: Double,
        var distance: Double,
        var vo2Max: Double,
        var elapsedTime: Double,
        var avgPace: Double,
        var currentPace: Double,
        var avgPower: Double,
        var currentPower: Double,
        var predictedMarathonFinishTime: String,
        var rollingMilePace: String,
        var songTitle: String,
        var songArtist: String
) {
}