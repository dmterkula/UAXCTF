package com.terkula.uaxctf.training.dto

import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.training.model.TargetedPace
import com.terkula.uaxctf.training.model.WorkoutComponent

class RunnerWorkoutPlanDTO (
    var runner: Runner,
    var baseTime: String,
    var targetedPaces: List<TargetedPace>
)

class RunnerWorkoutPlanDTOV2 (
    var runner: Runner,
    var componentPlans: List<WorkoutComponentPlanElement>
)


class ComponentToRunnerWorkoutPlans(
   var component: WorkoutComponent,
   var runnerWorkoutPlans: List<RunnerWorkoutPlanDTOV2>
)

class WorkoutComponentPlanElement(
    var distance: Int,
    var duration: String?,
    var baseTime: String,
    var targetedPace: List<TargetedPace>
)

class WorkoutComponentLabel(
    var distance: Int,
    var duration: String
)

class RunnerWorkoutTarget(
    var runner: Runner,
    var baseTime: String,
    var targetTime: String
)