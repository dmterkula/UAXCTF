package com.terkula.uaxctf.training.response

import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.training.dto.RunnerWorkoutResultsDTO
import com.terkula.uaxctf.training.dto.WorkoutResultDTO

class RunnerWorkoutResultsResponse (val runner: Runner, val workouts: List<RunnerWorkoutResultsDTO>)