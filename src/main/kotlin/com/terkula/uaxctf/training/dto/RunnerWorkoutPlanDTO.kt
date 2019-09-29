package com.terkula.uaxctf.training.dto

import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.training.model.TargetedPace

class RunnerWorkoutPlanDTO (val runner: Runner, val baseTime: String, val targetedPaces: List<TargetedPace>)