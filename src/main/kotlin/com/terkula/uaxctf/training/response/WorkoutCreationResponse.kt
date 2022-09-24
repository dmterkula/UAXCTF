package com.terkula.uaxctf.training.response

import com.terkula.uaxctf.training.dto.RunnerWorkoutPlanDTO
import com.terkula.uaxctf.training.model.Workout

class WorkoutCreationResponse (val workout: Workout, val workoutPlans: List<RunnerWorkoutPlanDTO>) {

}