package com.terkula.uaxctf.training.response

import com.fasterxml.jackson.annotation.JsonInclude
import com.terkula.uaxctf.statisitcs.model.Venue
import com.terkula.uaxctf.training.model.WorkoutComponent
import java.sql.Date

@JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
class WorkoutResponseDTO(
        var date: Date,
        var description: String,
        var title: String,
        var icon: String,
        var uuid: String,
        var components: List<WorkoutComponent>,
        var season: String,
        var team: String,
        var venue: Venue? = null,
        var assignedRunnerIds: List<Int>? = null
)