package com.terkula.uaxctf.training.request.crosstraining

import java.sql.Date

class CreateCrossTrainingRequest(
        val date: Date,
        val distance: Double?,
        val distanceUnit: String?,
        val duration: String?,
        val icon: String,
        val uuid: String,
        val name: String,
        val description: String?,
        val season: String,
        val team: String,
        val effortLabel: String?,
        val crossTrainingType: String
    ) {
}