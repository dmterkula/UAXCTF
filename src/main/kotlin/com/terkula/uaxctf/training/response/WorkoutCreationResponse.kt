package com.terkula.uaxctf.training.response

import com.terkula.uaxctf.training.model.WorkoutComponent
import com.terkula.uaxctf.training.model.Workout

class WorkoutCreationResponse(
        var workout: Workout,
        var components: List<WorkoutComponent>
        ) {

}