package com.terkula.uaxctf.training.response.crosstraining

import java.sql.Date

class CrossTrainingResponse(val crossTrainingActivities: List<CrossTrainingDTO>)

class CrossTrainingDTO(
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