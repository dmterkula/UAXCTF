package com.terkula.uaxctf.training.model.Firebase

import com.terkula.uaxctf.training.model.WorkoutComponent
import java.util.*

class WorkoutEvent(
        var id: UUID = UUID.randomUUID(),
        var date: Date,
        var title: String,
        var description: String,
        var icon: String,
        var uuid: String,
        var components: List<WorkoutComponent>,
        var season: String,
        var team: String
) {
}