package com.terkula.uaxctf.training.dto

class LiveWorkoutDTO(

        var uuid: String,

        var workoutUuid: String,
        var timestamp: String,
        var username: String,

        var heartRate: Int,

        var avgHeartRate: Int,

        var activeEnergy: Int,

        var vo2Max: Double,
        var distance: Double,

        var elapsedTime: String,

        var avgPace: String,

        var currentPace: String,

        var avgPower: Int,

        var currentPower: Int,

        var predictedMarathonFinishTime: String,

        var rollingMilePace: String,

        var songTitle: String,

        var songArtist: String,

        var mileSplits: List<String>
) {
}