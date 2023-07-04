package com.terkula.uaxctf.training.response

import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.training.dto.WorkoutResultDTO

class WorkoutResultResponse (val workoutResults: List<WorkoutResultDTO>)

class RunnerWorkoutResultResponse(val runner: Runner, val workout: WorkoutResponseDTO, val componentResults: List<SplitsResponse>,
                                  val totalDistance: Double, val time: String, val pace: String, val warmUpDistance: Double?,
                                  val warmUpTime: String?, val warmUpPace: String?,
                                  val coolDownDistance: Double?, val coolDownTime: String?, val coolDownPace: String?,
                                  val notes: String?)