package com.terkula.uaxctf.training.service


import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.statistics.controller.MeetPerformanceController
import com.terkula.uaxctf.statistics.exception.DuplicateWorkoutException
import com.terkula.uaxctf.statistics.exception.RunnerNotFoundByPartialNameException
import com.terkula.uaxctf.statistics.exception.WorkoutNotFoundException
import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.statistics.repository.TimeTrialRepository
import com.terkula.uaxctf.statistics.service.SeasonBestService
import com.terkula.uaxctf.statistics.service.XcGoalService
import com.terkula.uaxctf.training.dto.RunnerWorkoutResultsDTO
import com.terkula.uaxctf.training.dto.WorkoutResultDTO
import com.terkula.uaxctf.training.dto.WorkoutSplitsDTO
import com.terkula.uaxctf.training.model.NumberedSplit
import com.terkula.uaxctf.training.model.Workout
import com.terkula.uaxctf.training.model.WorkoutSplit
import com.terkula.uaxctf.training.model.calculateSpread
import com.terkula.uaxctf.training.repository.WorkoutRepository
import com.terkula.uaxctf.training.repository.WorkoutSplitRepository
import com.terkula.uaxctf.training.response.RunnerWorkoutResultsResponse
import com.terkula.uaxctf.util.calculateSecondsFrom
import com.terkula.uaxctf.util.getYearString
import com.terkula.uaxctf.util.toMinuteSecondString
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.sql.Date

@Component
class WorkoutResultService (
        @field:Autowired
        internal val workoutRepository: WorkoutRepository,
        @field:Autowired
        internal val workoutSplitRepository: WorkoutSplitRepository,
        @field: Autowired
        internal val runnerRepository: RunnerRepository,
        @field: Autowired
        internal val xcGoalService: XcGoalService,
        @field: Autowired
        internal val seasonBestService: SeasonBestService,
        @field:Autowired
        internal val timeTrialRepository: TimeTrialRepository) {

    fun getWorkoutResults(date: Date, type: String, targetDistance: Int, compareTo: String,
                          sortMethod: String): List<WorkoutResultDTO> {

//        val workouts = workoutRepository.findByDate(date)
//
//        val workout = if (workouts.isEmpty()) {
//            throw WorkoutNotFoundException("no workout found for the given date:  $date")
//        } else if (workouts.size == 1) {
//            workouts.first()
//        } else {
//            if (type.isNotEmpty()) {
//                if (workouts.filter { it.type.equals(type, ignoreCase = true) }.size in 1..workouts.size) {
//                    workouts.filter { it.type.equals(type, ignoreCase = true) }.first()
//                } else {
//                    throw DuplicateWorkoutException("multiple workouts found on the same date with the same type")
//                }
//            } else {
//                throw DuplicateWorkoutException("multiple workouts found on the same date")
//            }
//
//        }
//
//        val runners: Map<Int, Runner> = runnerRepository.findByGraduatingClassGreaterThan(MeetPerformanceController.CURRENT_YEAR)
//                .map { it.id to it }.toMap()
//
//        val workoutSplits = workoutSplitRepository.findByWorkoutId(workout.id)
//
//        val runnerToSplits = workoutSplits.groupBy { it.runnerId } .map {
//            runners[it.key] to it.value
//        }.toMap()
//
//        val distanceRatio = workout.targetDistance.toDouble() / 5000
//
//        var switch =  workout.pace
//        if (compareTo.isNotEmpty()) {
//            switch = compareTo
//        }
//
//        when (switch.toLowerCase()) {
//
//            "goal" -> {
//
//                val runnerGoals = xcGoalService.getGoalsForSeason(date.toString().substring(0,4))
//                        .map {
//                            it.runner.id to it.times
//                        }.toMap()
//
//                val workoutResult = runnerToSplits.map {
//                    val targetPace = (runnerGoals.getValue(it.key!!.id).first().calculateSecondsFrom() * distanceRatio).toMinuteSecondString()
//                    WorkoutResultDTO(it.key!!, buildWorkoutInfoFromSplits(it.value, targetPace, "Goal"))
//                }
//
//                return sortByMethod(workoutResult, sortMethod)
//            }
//
//            "race pace" -> {
//
//                val startDate = Date.valueOf(MeetPerformanceController.CURRENT_YEAR + "-01-01")
//
//                val seasonBests = seasonBestService.getAllSeasonBests(startDate, date, false)
//                        .map {
//                            it.runner.id to it.seasonBest.first().time
//                        }.toMap().filter { it.key in workoutSplits.map { split -> split.runnerId } }
//
//                val workoutResult = runnerToSplits.map {
//
//                    val seasonBest = seasonBests[it.key!!.id]
//
//                    val targetTime: String
//                    val source: String
//                    if (seasonBest.isNullOrBlank()) {
//                        val timeTrialResultsForRunner = timeTrialRepository.findByRunnerIdAndSeason(it.key!!.id, date.getYearString())
//                        if (timeTrialResultsForRunner.isEmpty()) {
//                            targetTime = default5k
//                            source = "default"
//                        } else {
//                            targetTime = timeTrialResultsForRunner.first().time
//                            source = "Time Trial"
//                        }
//                    } else {
//                        targetTime = seasonBests[it.key!!.id]!!
//                        source = "Season Best"
//                    }
//
//                    val targetPace = (targetTime.calculateSecondsFrom() * distanceRatio).toMinuteSecondString()
//                    WorkoutResultDTO(it.key!!, buildWorkoutInfoFromSplits(it.value, targetPace, source))
//
//                }
//
//                return sortByMethod(workoutResult, sortMethod)
//            }
//
//            else -> {
//
//                val workoutResult = runnerToSplits.map {
//                    WorkoutResultDTO(it.key!!, buildWorkoutInfoFromSplits(it.value, "n/a", "n/a"))
//
//                }
//
//                return sortByMethod(workoutResult, sortMethod)
//
//            }
//
//        }

        return emptyList()
    }

    fun getWorkoutsForRunner(startDate: Date, endDate: Date, name: String, distance: Int, type: String, sortMethod: String): RunnerWorkoutResultsResponse {

        val runner: Runner
        try {
            runner = runnerRepository.findByNameContaining(name).first()
        } catch (e: Exception) {
            throw RunnerNotFoundByPartialNameException("Unable to find results for runner by: $name")
        }

        var workouts= workoutRepository.findByDateBetween(startDate, endDate).map { it.id to it }.toMap()
        if (distance != 0) {
            workouts = workouts.filter { it.value.targetDistance == distance }
        }

        if (type.isNotEmpty()) {
            workouts =  workouts.filter { it.value.type.toLowerCase() == type.toLowerCase() }
        }

        val workoutsForRunner = workouts.map { workoutSplitRepository.findByWorkoutIdAndRunnerId(it.key, runner.id) }
                .flatten()
                .groupBy { it.workoutId }
                .map { workouts[it.key]!! to it.value }.toMap()
                // pair workout to the summary info that workout
                .map {
                    val distanceRatio =  it.key.targetDistance.toDouble() / 5000
                    val season = it.key.date.getYearString()
                    val targetPace = getTargetPaceForRunner(it.key, distanceRatio, runner, season)
                    it.key to buildWorkoutInfoFromSplits(it.value, targetPace.first, targetPace.second)
                }.toMap()
                .map {
                    RunnerWorkoutResultsDTO(it.key, it.value)
                }.toMutableList()

        return RunnerWorkoutResultsResponse(runner, sortRunnerWorkoutsByMethod(workoutsForRunner, sortMethod))

    }

    fun getEveryRunnerWhoHasRanAWorkout(startDate: Date, endDate: Date): List<Runner> {
        return workoutRepository.findByDateBetween(startDate, endDate)
                .map { workoutSplitRepository.findByWorkoutId(it.id) }
                .flatten()
                .map { it.runnerId }
                .distinct()
                .map {runnerRepository.findById(it).get()}

    }

    private fun buildWorkoutInfoFromSplits(splits: List<WorkoutSplit>, targetPace: String, source: String): WorkoutSplitsDTO {

        val spread = splits.calculateSpread().toMinuteSecondString()
        val numberedSplits = splits.map { NumberedSplit(it.splitNumber, it.time) }
        return WorkoutSplitsDTO(numberedSplits, targetPace, source, spread, (numberedSplits
                .map { split -> split.time.calculateSecondsFrom() }.average() - targetPace.calculateSecondsFrom()).toMinuteSecondString())

    }

    private fun sortByMethod(workouts: List<WorkoutResultDTO>, sortMethod: String): List<WorkoutResultDTO> {
        return when (sortMethod) {
            "spread" -> workouts.toMutableList().sortedBy { it.workoutResults.spread }
            "target" -> workouts.toMutableList().sortedBy { it.workoutResults.targetPace}
            else -> workouts.toMutableList().sortedBy { it.workoutResults.avgDifferenceFromTarget.calculateSecondsFrom() }
        }
    }


    private fun sortRunnerWorkoutsByMethod(workouts: List<RunnerWorkoutResultsDTO>, sortMethod: String): List<RunnerWorkoutResultsDTO> {
        return when (sortMethod) {
            "spread" -> workouts.toMutableList().sortedBy { it.workoutResultDTO.spread }
            "target" -> workouts.toMutableList().sortedBy { it.workoutResultDTO.targetPace}
            else -> workouts.toMutableList().sortedBy { it.workoutResultDTO.avgDifferenceFromTarget.calculateSecondsFrom() }
        }
    }

    private fun getTargetPaceForRunner(workout: Workout, distanceRatio: Double, runner: Runner, season: String): Pair<String, String> {

        return Pair("", "")

        // todo

//        return when (workout.pace.toLowerCase()) {
//            "race" -> {
//
//                val startDate = Date.valueOf("$season-01-01")
//                val endDate = Date.valueOf("$season-12-31")
//
//                val seasonBests = seasonBestService.getSeasonBestsByName(runner.name, listOf(startDate to endDate), false)
//
//                val targetPace = if (seasonBests.isEmpty() || seasonBests.first().seasonBest.isEmpty()) {
//                    val timeTrialResultsForRunner = timeTrialRepository.findByRunnerIdAndSeason(runner.id, season)
//                    if (timeTrialResultsForRunner.isEmpty()) {
//                        (default5k.calculateSecondsFrom() * distanceRatio).toMinuteSecondString() to "default"
//                    } else {
//                        (timeTrialResultsForRunner.first().time.calculateSecondsFrom() * distanceRatio).toMinuteSecondString() to "Time Trial"
//                    }
//                } else {
//                    (seasonBests.first().seasonBest.first().time.calculateSecondsFrom() * distanceRatio).toMinuteSecondString() to "Season Best"
//                }
//
//                targetPace
//            }
//
//            "goal" -> {
//                (xcGoalService.getRunnersGoalForSeason(runner.name, season).first().times.first().calculateSecondsFrom() * distanceRatio)
//                        .toMinuteSecondString() to "Goal"
//
//            } else -> {
//                (default5k.calculateSecondsFrom() * distanceRatio).toMinuteSecondString() to "Default"
//            }
//
//        }

    }

    companion object {
        const val default5k = "25:00"
    }

}
