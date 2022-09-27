package com.terkula.uaxctf.training.service

import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.statisitcs.model.toMeetPerformanceDTO
import com.terkula.uaxctf.statistics.controller.MeetPerformanceController
import com.terkula.uaxctf.statistics.dto.RunnerGoalDTO
import com.terkula.uaxctf.statistics.repository.MeetPerformanceRepository
import com.terkula.uaxctf.statistics.repository.MeetRepository
import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.statistics.request.SortingMethodContainer
import com.terkula.uaxctf.statistics.service.PersonalRecordService
import com.terkula.uaxctf.statistics.service.SeasonBestService
import com.terkula.uaxctf.statistics.service.XcGoalService
import com.terkula.uaxctf.training.dto.RunnerWorkoutPlanDTO
import com.terkula.uaxctf.training.model.TargetedPace
import com.terkula.uaxctf.training.model.Workout
import com.terkula.uaxctf.training.repository.WorkoutRepository
import com.terkula.uaxctf.training.response.WorkoutCreationMetadata
import com.terkula.uaxctf.training.response.WorkoutCreationResponse
import com.terkula.uaxctf.util.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.lang.RuntimeException
import java.sql.Date

@Component
class WorkoutService (
        @field:Autowired
        internal var seasonBestService: SeasonBestService,
        @field:Autowired
        internal var prService: PersonalRecordService,
        @field:Autowired
        internal var runnerRepository: RunnerRepository,
        @field:Autowired
        internal var meetRepository: MeetRepository,
        @field:Autowired
        internal var meetPerformanceRepository: MeetPerformanceRepository,
        @field:Autowired
        internal var xcGoalService: XcGoalService,
        @field:Autowired
        internal var workoutRepository: WorkoutRepository
) {

    fun getWorkouts(startDate: Date, endDate: Date): List<Workout> {

        val workouts =
                workoutRepository.findByDateBetween(startDate, endDate)

        workouts.forEach { it.date.addDay() }

        return workouts

    }

    fun deleteWorkout(uuid: String): Workout? {
        val workoutToDelete = workoutRepository.findByUuid(uuid).firstOrNull()

        if (workoutToDelete != null) {
            workoutRepository.delete(workoutToDelete)
        }

        return workoutToDelete
    }

    fun updateWorkout(
        uuid: String,
        date: Date,
        type: String,
        title: String,
        description: String,
        distance: Int,
        targetCount: Int,
        pace: String,
        duration: String,
        icon: String
    ): Workout? {

        val workoutToUpdate = workoutRepository.findByUuid(uuid).firstOrNull()

        if (workoutToUpdate != null) {
            workoutToUpdate.date = date
            workoutToUpdate.title = title
            workoutToUpdate.description = description
            workoutToUpdate.type = type
            workoutToUpdate.pace = pace
            workoutToUpdate.targetCount = targetCount
            workoutToUpdate.targetDistance = distance
            workoutToUpdate.icon = icon


            workoutRepository.save(workoutToUpdate)
        }

        return workoutToUpdate
    }

    fun createWorkout(
            date: Date,
            type: String,
            title: String,
            description: String,
            distance: Int,
            targetCount: Int,
            pace: String,
            duration: String,
            icon: String,
            uuid: String
    ): WorkoutCreationResponse? {

        val startDate = Date.valueOf("${date.getYearString()}-01-01")
        val endDate = Date.valueOf((date.getYearString()) + "-12-31")

        if (workoutRepository.findByDate(date).firstOrNull()?.title == title) {
            throw RuntimeException("Workout with that date and title already exists")
        }

        when (type) {
            "Interval" -> {
                val distanceRatio = distance.toDouble() / fiveK.toDouble()
                when (pace) {
                    "Goal" -> {

                        val seasonGoals = xcGoalService.getGoalsForSeason(date.getYearString())
                                .map {
                                    RunnerGoalDTO(it.runner, it.goals.filter{ goal-> goal.type.equals("time", ignoreCase = true) }.sortedBy{goal->goal.value})
                                }
                                .filter {
                                    it.goals.isNotEmpty()
                                }
                        val workoutPlanDTOs =  seasonGoals.map { RunnerWorkoutPlanDTO(it.runner, it.goals.first().value, listOf(TargetedPace("split",
                                (it.goals.first().value.calculateSecondsFrom() * distanceRatio).toMinuteSecondString()))) }.toMutableList().sortedBy { it.baseTime }


                        val workout = Workout(date, type, description, distance, targetCount, pace, duration, title, icon, uuid)
                        workoutRepository.save(workout)
                        return WorkoutCreationResponse(workout, workoutPlanDTOs)


                    }
                    "SB" -> {

                        val seasonBests = seasonBestService.getAllSeasonBests(startDate, endDate, false)

                        val workoutPlanDTOs = seasonBests.map {
                            RunnerWorkoutPlanDTO(it.runner, it.seasonBest.first().time, listOf(TargetedPace("split",
                                    (it.seasonBest.first().time.calculateSecondsFrom() * (distanceRatio)).round(2).toMinuteSecondString())))
                        }

                        val workout = Workout(date, type, description, distance, targetCount, pace, duration, title, icon, uuid)
                        workoutRepository.save(workout)
                        return WorkoutCreationResponse(workout, workoutPlanDTOs)

                    }
                    "PR" -> {
                        val gradClass = date.getYearString().toInt().toString()

                        val prs = prService.getAllPRs(gradClass, "", SortingMethodContainer.TIME, false)

                        val workoutPlanDTOs = prs.map {
                            RunnerWorkoutPlanDTO(it.runner, it.pr.first().time, listOf(TargetedPace("split",
                                    (it.pr.first().time.calculateSecondsFrom() * (distanceRatio)).round(2).toMinuteSecondString())))
                        }.toList()

                        val workout = Workout(date, type, description, distance, targetCount, pace, duration, title, icon, uuid)
                        workoutRepository.save(workout)
                        return WorkoutCreationResponse(workout, workoutPlanDTOs)


                    }
                    "Season Avg" -> {

                        // find all runners whose graduating class is > current year
                        val eligibleRunners = runnerRepository.findByGraduatingClassGreaterThan(date.getYearString()).map { it.id to it }.toMap()

                        val workoutPlans = getSeasonAverages(eligibleRunners, startDate, endDate).map {
                            RunnerWorkoutPlanDTO(eligibleRunners[it.key]!!,
                                    it.value.first().toMinuteSecondString(), listOf(TargetedPace("split", (distanceRatio * it.value.first()).toMinuteSecondString())))
                        }

                        val workout = Workout(date, type, description, distance, targetCount, pace, duration, title, icon, uuid)
                        workoutRepository.save(workout)
                        return WorkoutCreationResponse(workout, workoutPlans)

                    }
                }

            }
            "Tempo" -> {
                val distanceRatio = 1609.0/5000.0
                when (pace) {
                    "Goal" -> {

                        val seasonGoals = xcGoalService.getGoalsForSeason(date.getYearString())
                                .map {
                                    RunnerGoalDTO(it.runner, it.goals.filter{ goal-> goal.value.equals("time", ignoreCase = true) }.sortedBy{goal->goal.value})
                                }
                                .filter {
                                    it.goals.isNotEmpty()
                                }

                        val workoutPlanDTOs =  seasonGoals.map { RunnerWorkoutPlanDTO(it.runner, it.goals.first().value, listOf(TargetedPace("split",
                                (it.goals.first().value.calculateSecondsFrom() * distanceRatio).toMinuteSecondString()))) }.toMutableList().sortedBy { it.baseTime }

                        val workout = Workout(date, type, description, distance, targetCount, pace, duration, title, icon, uuid)
                        workoutRepository.save(workout)
                        return WorkoutCreationResponse(workout, workoutPlanDTOs)

                    }
                    "SB" -> {

                        val seasonBests = seasonBestService.getAllSeasonBests(startDate, endDate, false)


                        val workoutPlanDTOs = seasonBests.map {
                            RunnerWorkoutPlanDTO(it.runner, it.seasonBest.first().time, listOf(TargetedPace("perMile", ((it.seasonBest.first().time
                                    .calculateSecondsFrom() * distanceRatio + tempoScale).round(2).toMinuteSecondString()))))
                        }

                        val workout = Workout(date, type, description, distance, targetCount, pace, duration, title, icon, uuid)
                        workoutRepository.save(workout)
                        return WorkoutCreationResponse(workout, workoutPlanDTOs)

                    }
                    "PR" -> {
                        val gradClass = date.getYearString().toInt().toString()

                        val prs = prService.getAllPRs(gradClass, "", SortingMethodContainer.TIME, false)
                        val workoutPlanDTOs = prs.map {
                            RunnerWorkoutPlanDTO(it.runner, it.pr.first().time, listOf(TargetedPace("perMile", (it.pr.first().time.calculateSecondsFrom() * distanceRatio + tempoScale).round(2).toMinuteSecondString())))
                        }.toList()

                        val workout = Workout(date, type, description, distance, targetCount, pace, duration, title, icon, uuid)
                        workoutRepository.save(workout)
                        return WorkoutCreationResponse(workout, workoutPlanDTOs)


                    }
                    "Season Avg" -> {
                        // find all runners whose graduating class is > current year
                        val eligibleRunners = runnerRepository.findByGraduatingClassGreaterThan(date.getYearString()).map { it.id to it }.toMap()

                        val workoutPlans = getSeasonAverages(eligibleRunners, startDate, endDate).map {
                            RunnerWorkoutPlanDTO(eligibleRunners[it.key]!!,
                                    it.value.first().toMinuteSecondString(), listOf(TargetedPace("perMile", (distanceRatio * it.value.first() + tempoScale).toMinuteSecondString())))
                        }

                        val workout = Workout(date, type, description, distance, targetCount, pace, duration, title, icon, uuid)
                        workoutRepository.save(workout)
                        return WorkoutCreationResponse(workout, workoutPlans)
                    }
                }
            }
            "Progression" -> {
                val distanceRatio = 1609.0/5000.0

                when (pace) {
                    "Goal" -> {

                        val seasonGoals = xcGoalService.getGoalsForSeason(date.getYearString())
                                .map {
                                    RunnerGoalDTO(it.runner, it.goals.filter{ goal-> goal.value.equals("time", ignoreCase = true) }.sortedBy{goal->goal.value})
                                }
                                .filter {
                                    it.goals.isNotEmpty()
                                }

                        val workoutPlanDTOs = seasonGoals.map {
                            val baseTimePerMile = it.goals.first().value.calculateSecondsFrom() * distanceRatio
                            RunnerWorkoutPlanDTO(it.runner, it.goals.first().value, constructProgressionTargetedPaces(baseTimePerMile))
                        }


                        val workout = Workout(date, type, description, distance, targetCount, pace, duration, title, icon, uuid)
                        workoutRepository.save(workout)
                        return WorkoutCreationResponse(workout, workoutPlanDTOs)

                    }
                    "SB" -> {

                        val seasonBests = seasonBestService.getAllSeasonBests(startDate, endDate, false)


                        val workoutPlanDTOs = seasonBests.map {
                            val baseTimePerMile = it.seasonBest.first().time.calculateSecondsFrom() * distanceRatio
                            RunnerWorkoutPlanDTO(it.runner, it.seasonBest.first().time, constructProgressionTargetedPaces(baseTimePerMile))
                        }

                        val workout = Workout(date, type, description, distance, targetCount, pace, duration, title, icon, uuid)
                        workoutRepository.save(workout)
                        return WorkoutCreationResponse(workout, workoutPlanDTOs)

                    }
                    "PR" -> {
                        val gradClass = date.getYearString().toInt().toString()

                        val prs = prService.getAllPRs(gradClass, "", SortingMethodContainer.TIME, false)
                        val workoutPlanDTOs = prs.map {
                            val baseTimePerMile = it.pr.first().time.calculateSecondsFrom() * distanceRatio
                            RunnerWorkoutPlanDTO(it.runner, it.pr.first().time, constructProgressionTargetedPaces(baseTimePerMile))
                        }.toList()

                        val workout = Workout(date, type, description, distance, targetCount, pace, duration, title, icon, uuid)
                        workoutRepository.save(workout)
                        return WorkoutCreationResponse(workout, workoutPlanDTOs)


                    }
                    "Season Avg" -> {
                        // find all runners whose graduating class is > current year
                        val eligibleRunners = runnerRepository.findByGraduatingClassGreaterThan(date.getYearString()).map { it.id to it }.toMap()

                        val workoutPlans = getSeasonAverages(eligibleRunners, startDate, endDate) .map {
                            val basePacePerMile = distanceRatio * it.value.first()
                            RunnerWorkoutPlanDTO(eligibleRunners[it.key]!!,
                                    it.value.first().toMinuteSecondString(), constructProgressionTargetedPaces(basePacePerMile))
                        }

                        val workout = Workout(date, type, description, distance, targetCount, pace, duration, title, icon, uuid)
                        workoutRepository.save(workout)
                        return WorkoutCreationResponse(workout, workoutPlans)
                    }
                }

            }
            "descriptionOnly" -> {
                val workout = Workout(date, type, description, distance, targetCount, pace, duration, title, icon, uuid)
                workoutRepository.save(workout)
                return WorkoutCreationResponse(workout, emptyList())
            }
            else -> {
                return null
            }
        }

        return null

    }

    fun getSeasonAverages(eligibleRunners: Map<Int, Runner>, startDate: Date, endDate: Date): Map<Int, List<Double>> {

        val meets = meetRepository.findByDateBetween(startDate, endDate)
                .toMutableList().sortedByDescending { it.date }

        val meetMap = meets.map { it.id to it }.toMap()

        return eligibleRunners.map { meetPerformanceRepository.findByRunnerId(it.key) }
                .flatten().groupBy { it.runnerId }
                .map {
                    it.key to it.value.
                            filter { perf -> perf.meetId in meetMap }
                            .toMutableList()
                            .sortedByDescending { perf -> meetMap[perf.meetId]!!.date }
                }.toMap()
                .map { it.key to it.value.toMeetPerformanceDTO(meetMap) }.toMap()
                .map {
                    val numMeets = it.value.size
                    it.key to listOf(it.value.map { perf -> perf.time.calculateSecondsFrom() }.sum()/numMeets)
                }.toMutableList().sortedBy { it.second.first() }.toMap()
    }

    fun constructProgressionTargetedPaces(basePacePerMile: Double): List<TargetedPace> {
        val controlledTempoScale = tempoScale + 25
        val steadyState = controlledTempoScale + 25

        return listOf(TargetedPace("Steady State", (basePacePerMile + steadyState).round(2).toMinuteSecondString()),
                TargetedPace("Controlled Tempo", ((basePacePerMile + controlledTempoScale).round(2)).toMinuteSecondString()),
                TargetedPace("Tempo", ((basePacePerMile + tempoScale).round(2)).toMinuteSecondString()))

    }

    companion object {
        const val fiveK: Int = 5000
        const val tempoScale = 45.0
    }

}