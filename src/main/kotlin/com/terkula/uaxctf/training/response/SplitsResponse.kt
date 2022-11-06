package com.terkula.uaxctf.training.response

import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.training.model.WorkoutComponent

class SplitsResponse(
        val componentUUID: String,
        val runnerId: Int,
        val splits: List<SplitResponse>) {


}

class SplitResponse(val uuid: String, val number: Int, val time: String)

class RunnerWorkoutSplits(val runner: Runner, val splits: List<SplitResponse>, val average: String)

class ComponentSplitsResponse(val component: WorkoutComponent, val results: List<RunnerWorkoutSplits>)