package com.terkula.uaxctf.training.dto

import java.sql.Timestamp
import javax.persistence.Column
import javax.persistence.Id

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

        var currentPower: Int
) {
}