package com.terkula.uaxctf.training.response

import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.training.dto.WorkoutResultDTO
import com.terkula.uaxctf.training.model.Workout

class WorkoutResultResponse (val workoutResults: List<WorkoutResultDTO>)

class RunnerWorkoutResultResponse(val runner: Runner, val workout: Workout, val componentResults: List<SplitsResponse>, val totalDistance: Double)