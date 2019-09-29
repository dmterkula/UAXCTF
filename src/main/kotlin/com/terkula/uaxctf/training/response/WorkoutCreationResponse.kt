package com.terkula.uaxctf.training.response

import com.terkula.uaxctf.training.dto.RunnerWorkoutPlanDTO

class WorkoutCreationResponse (val requestInfo: WorkoutCreationMetadata, val workoutPlans: List<RunnerWorkoutPlanDTO>) {

}