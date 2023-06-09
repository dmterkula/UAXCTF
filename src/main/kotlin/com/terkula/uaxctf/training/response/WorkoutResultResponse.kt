package com.terkula.uaxctf.training.response

import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.training.dto.WorkoutResultDTO

class WorkoutResultResponse (val workoutResults: List<WorkoutResultDTO>)

class RunnerWorkoutResultResponse(val runner: Runner, val workout: WorkoutResponseDTO, val componentResults: List<SplitsResponse>,
                                  val totalDistance: Double, val notes: String?)