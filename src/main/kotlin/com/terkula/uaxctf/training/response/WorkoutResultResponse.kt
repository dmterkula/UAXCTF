package com.terkula.uaxctf.training.response

import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.training.dto.WorkoutResultDTO
import com.terkula.uaxctf.training.model.TrainingComment
import com.terkula.uaxctf.util.calculateSecondsFrom

class WorkoutResultResponse (val workoutResults: List<WorkoutResultDTO>)

class RunnerWorkoutResultResponse(val runner: Runner, val workout: WorkoutResponseDTO, val componentResults: List<SplitsResponse>,
                                  val totalDistance: Double, val time: String, val pace: String, val warmUpDistance: Double?,
                                  val warmUpTime: String?, val warmUpPace: String?,
                                  val coolDownDistance: Double?, val coolDownTime: String?, val coolDownPace: String?,
                                  val notes: String?, val coachNotes: String?, val comments: List<TrainingComment>)

fun RunnerWorkoutResultResponse.getTotalDistance(): Double {

    var dist = this.totalDistance

    if (warmUpDistance != null) {
        dist += warmUpDistance!!
    }

    if (coolDownDistance != null) {
        dist += coolDownDistance!!
    }

    return dist
}

fun RunnerWorkoutResultResponse.getTotalSeconds(): Double {

    var totalTime = this.time.calculateSecondsFrom()

    if (warmUpDistance != null && warmUpTime != null) {
        totalTime += warmUpTime.calculateSecondsFrom()
    }

    if (coolDownDistance != null && coolDownTime != null) {
        totalTime += coolDownTime.calculateSecondsFrom()
    }

    return totalTime
}