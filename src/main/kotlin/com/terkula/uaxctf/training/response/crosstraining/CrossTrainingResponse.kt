package com.terkula.uaxctf.training.response.crosstraining

import com.fasterxml.jackson.annotation.JsonInclude
import com.terkula.uaxctf.statisitcs.model.Venue
import java.sql.Date

class CrossTrainingResponse(val crossTrainingActivities: List<CrossTrainingDTO>)

@JsonInclude(JsonInclude.Include.NON_NULL)
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
        val crossTrainingType: String,
        val venue: Venue? = null,
        val assignedRunnerIds: List<Int>? = null
) {
}