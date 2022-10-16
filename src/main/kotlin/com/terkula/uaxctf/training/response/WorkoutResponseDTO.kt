package com.terkula.uaxctf.training.response

import com.terkula.uaxctf.training.model.WorkoutComponent
import java.sql.Date

class WorkoutResponseDTO(
        var date: Date,
        var description: String,
        var title: String,
        var icon: String,
        var uuid: String,
        var components: List<WorkoutComponent>
)