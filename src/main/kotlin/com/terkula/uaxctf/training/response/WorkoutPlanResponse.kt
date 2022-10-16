package com.terkula.uaxctf.training.response

import com.terkula.uaxctf.training.dto.*

class WorkoutPlanResponse(val workoutPlans: List<RunnerWorkoutPlanDTO>)


class WorkoutPlanResponseV2(
       var componentsToRunnerWorkoutPlans: List<ComponentToRunnerWorkoutPlans>,
       var runnerWorkoutPlanDTOV2: List<RunnerWorkoutPlanDTOV2>
    )

class WorkoutComponentElementToRunnerPlans(
        var componentLabel: WorkoutComponentLabel,
        var runnerPlans: List<RunnerWorkoutTarget>

)