package com.terkula.uaxctf.training.service

import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.statisitcs.model.toMeetPerformanceDTO
import com.terkula.uaxctf.statistics.dto.RunnerGoalDTO
import com.terkula.uaxctf.statistics.repository.MeetPerformanceRepository
import com.terkula.uaxctf.statistics.repository.MeetRepository
import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.statistics.request.SortingMethodContainer
import com.terkula.uaxctf.statistics.service.PersonalRecordService
import com.terkula.uaxctf.statistics.service.RunnerService
import com.terkula.uaxctf.statistics.service.SeasonBestService
import com.terkula.uaxctf.statistics.service.XcGoalService
import com.terkula.uaxctf.training.dto.ComponentToRunnerWorkoutPlans
import com.terkula.uaxctf.training.dto.RunnerWorkoutPlanDTOV2
import com.terkula.uaxctf.training.dto.WorkoutComponentPlanElement
import com.terkula.uaxctf.training.model.TargetedPace
import com.terkula.uaxctf.training.model.Workout
import com.terkula.uaxctf.training.model.WorkoutComponent
import com.terkula.uaxctf.training.repository.WorkoutComponentRepository
import com.terkula.uaxctf.training.repository.WorkoutRepository
import com.terkula.uaxctf.training.request.CreateWorkoutRequest
import com.terkula.uaxctf.training.response.*
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
        internal var workoutRepository: WorkoutRepository,
        var workoutRepositoryV2: WorkoutRepository,
        var workoutComponentRepository: WorkoutComponentRepository,
        var runnerService: RunnerService
) {

    fun getWorkoutsV2(startDate: Date, endDate: Date): List<WorkoutResponseDTO> {

        val workouts =
                workoutRepositoryV2.findByDateBetween(startDate, endDate)

        workouts.forEach { it.date.addDay() }

        return workouts.map {
            WorkoutResponseDTO(
                    it.date,
                    it.description,
                    it.title,
                    it.icon,
                    it.uuid,
                    workoutComponentRepository.findByWorkoutUuid(it.uuid))
        }

    }

    fun createWorkoutV2(
           createWorkoutRequest: CreateWorkoutRequest
    ): WorkoutResponseDTO {

        if (workoutRepository.findByDate(createWorkoutRequest.date).firstOrNull()?.title == createWorkoutRequest.title) {
            throw RuntimeException("Workout with that date and title already exists")
        } else {
           val workoutV2 = Workout(createWorkoutRequest.date, createWorkoutRequest.description, createWorkoutRequest.title,
           createWorkoutRequest.icon, createWorkoutRequest.uuid)

           val workoutComponents: List<WorkoutComponent> = createWorkoutRequest.components.map {
               WorkoutComponent(it.uuid, workoutV2.uuid, it.type, it.description, it.targetDistance,
                       it.targetCount, it.pace, it.duration, it.targetPaceAdjustment)
           }

            workoutRepositoryV2.save(workoutV2)
            workoutComponentRepository.saveAll(workoutComponents)

            return WorkoutResponseDTO(
                    workoutV2.date,
                    workoutV2.description,
                    workoutV2.title,
                    workoutV2.icon,
                    workoutV2.uuid,
                    workoutComponents)
        }

    }

    fun updateWorkoutV2(
        uuid: String,
        createWorkoutRequest: CreateWorkoutRequest
    ): WorkoutResponseDTO {

        val workoutToUpdate = workoutRepositoryV2.findByUuid(uuid).firstOrNull()

        if (workoutToUpdate != null) {

            workoutToUpdate.date = createWorkoutRequest.date
            workoutToUpdate.title = createWorkoutRequest.title
            workoutToUpdate.description = createWorkoutRequest.description
            workoutToUpdate.icon = createWorkoutRequest.icon
            workoutRepositoryV2.save(workoutToUpdate)


            var updatedComponents: List<WorkoutComponent?> = createWorkoutRequest.components.map {
                val component = workoutComponentRepository.findByUuid(it.uuid).firstOrNull()

                if (component != null) {
                    component.description = it.description
                    component.pace = it.pace
                    component.duration = it.duration
                    component.targetCount = it.targetCount
                    component.targetDistance = it.targetDistance
                    component.type =  it.type
                    component.targetPaceAdjustment = it.targetPaceAdjustment
                }

                return@map component

            }

           workoutComponentRepository.saveAll(updatedComponents.filter { it != null })

           return return WorkoutResponseDTO(
                   workoutToUpdate.date,
                   workoutToUpdate.description,
                   workoutToUpdate.title,
                   workoutToUpdate.icon,
                   workoutToUpdate.uuid,
                   updatedComponents.filterNotNull()
           )
        } else {
            throw (RuntimeException("Workout not found"))
        }

    }

    fun deleteWorkoutV2(uuid: String): WorkoutResponseDTO? {
        val workoutToDelete = workoutRepositoryV2.findByUuid(uuid).firstOrNull()

        return if (workoutToDelete != null) {

            val components = workoutComponentRepository.findByWorkoutUuid(uuid)

            workoutRepositoryV2.delete(workoutToDelete)
            workoutComponentRepository.deleteAll(components)

            return WorkoutResponseDTO(
                    workoutToDelete.date,
                    workoutToDelete.description,
                    workoutToDelete.title,
                    workoutToDelete.icon,
                    workoutToDelete.uuid,
                    components)
        } else {
            null
        }
    }

    fun getWorkoutPlanV2(uuid: String): WorkoutPlanResponseV2 {

        val workout = workoutRepositoryV2.findByUuid(uuid).firstOrNull() ?: return WorkoutPlanResponseV2(emptyList(), emptyList())

        val components = workoutComponentRepository.findByWorkoutUuid(workout.uuid)


       val componentPlans: List<ComponentToRunnerWorkoutPlans> = components.map {
           ComponentToRunnerWorkoutPlans(it, buildComponentPlan(workout, it))
       }

       val runnerWorkoutPlans: List<RunnerWorkoutPlanDTOV2> = componentPlans.map {
            it.runnerWorkoutPlans.map { runnerPlan -> runnerPlan to it.component }
        }.flatten()
                .groupBy {
                    it.first.runner
                }
                .map {
                    it.key to it.value.map { pair-> pair.first }
                }
                .map {
                    RunnerWorkoutPlanDTOV2(it.first, it.second.map { dto-> dto.componentPlans[0] })
                }

        return WorkoutPlanResponseV2(componentPlans, runnerWorkoutPlans)
    }

    fun buildComponentPlan(
            workout: Workout,
            component: WorkoutComponent
    ): List<RunnerWorkoutPlanDTOV2> {

        val date = workout.date
        val type = component.type
        val pace = component.pace
        val targetPaceAdjustment = component.targetPaceAdjustment

        var paceAdjustment: Int = if (targetPaceAdjustment.isEmpty()) {
            0
        } else {
            targetPaceAdjustment.calculateSecondsFrom().toInt()
        }

        val distance = component.targetDistance
        val startDate = Date.valueOf("${date.getYearString()}-01-01")
        val endDate = Date.valueOf((date.getYearString()) + "-12-31")

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

                        val runnerWorkoutPlanDTOV2: List<RunnerWorkoutPlanDTOV2> = seasonGoals.map {
                            RunnerWorkoutPlanDTOV2(it.runner,
                                    listOf(WorkoutComponentPlanElement(
                                            distance,
                                            component.duration,
                                            (it.goals.first().value.calculateSecondsFrom() + paceAdjustment).toMinuteSecondString(),
                                            listOf(TargetedPace("split", ((it.goals.first().value.calculateSecondsFrom() * distanceRatio) + paceAdjustment).toMinuteSecondString()))
                                        )
                                    )
                            )
                        }.toMutableList().sortedBy { it.componentPlans[0].baseTime }
                        return runnerWorkoutPlanDTOV2

                    }
                    "SB" -> {

                        val seasonBests = seasonBestService.getAllSeasonBests(startDate, endDate, false)

                        val runnerWorkoutPlanDTOV2: List<RunnerWorkoutPlanDTOV2> = seasonBests.map {
                            RunnerWorkoutPlanDTOV2(it.runner,
                                    listOf(WorkoutComponentPlanElement(
                                            distance,
                                            component.duration,
                                            (it.seasonBest.first().time.calculateSecondsFrom() + paceAdjustment).toMinuteSecondString(),
                                            listOf(TargetedPace("split", ((it.seasonBest.first().time.calculateSecondsFrom() + paceAdjustment) * distanceRatio).round(2).toMinuteSecondString()))
                                    )
                                )
                            )
                        }

                        return runnerWorkoutPlanDTOV2

                    }
                    "PR" -> {
                        val gradClass = date.getYearString().toInt().toString()

                        val prs = prService.getAllPRs(gradClass, "", SortingMethodContainer.TIME, false)

                        val runnerWorkoutPlanDTOV2: List<RunnerWorkoutPlanDTOV2> = prs.map {
                            RunnerWorkoutPlanDTOV2(it.runner,
                                    listOf(WorkoutComponentPlanElement(
                                            distance,
                                            component.duration,
                                            (it.pr.first().time.calculateSecondsFrom() + paceAdjustment).toMinuteSecondString(),
                                            listOf(TargetedPace("split", (it.pr.first().time.calculateSecondsFrom() * (distanceRatio) + paceAdjustment).round(2).toMinuteSecondString()))
                                    )
                                )
                            )
                        }

                        return runnerWorkoutPlanDTOV2
                    }
                    "Season Avg" -> {

                        // find all runners whose graduating class is > current year
                        val eligibleRunners = runnerService.getRoster(true, workout.date.getYearString()).map { it.id to it }.toMap()

                        val runnerWorkoutPlanDTOV2: List<RunnerWorkoutPlanDTOV2> = getSeasonAverages(eligibleRunners, startDate, endDate).map {
                            RunnerWorkoutPlanDTOV2(eligibleRunners[it.key]!!,
                                    listOf(WorkoutComponentPlanElement(
                                            distance,
                                            component.duration,
                                            (it.value.first().toMinuteSecondString().calculateSecondsFrom() + paceAdjustment).toMinuteSecondString(),
                                            listOf(TargetedPace("split", (distanceRatio * it.value.first() + paceAdjustment).toMinuteSecondString()))
                                    )
                                )
                            )
                        }

                        return runnerWorkoutPlanDTOV2
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

                        val runnerWorkoutPlanDTOV2: List<RunnerWorkoutPlanDTOV2> = seasonGoals.map {
                            RunnerWorkoutPlanDTOV2(it.runner,
                                    listOf(WorkoutComponentPlanElement(
                                            distance,
                                            component.duration,
                                            (it.goals.first().value.calculateSecondsFrom() + paceAdjustment).toMinuteSecondString(),
                                            listOf(TargetedPace("split", (it.goals.first().value.calculateSecondsFrom() * distanceRatio + tempoScale + paceAdjustment).toMinuteSecondString()))
                                    )
                                )
                            )
                        }.toMutableList().sortedBy { it.componentPlans[0].baseTime }
                        return runnerWorkoutPlanDTOV2

                    }
                    "SB" -> {

                        val seasonBests = seasonBestService.getAllSeasonBests(startDate, endDate, false)

                        val runnerWorkoutPlanDTOV2: List<RunnerWorkoutPlanDTOV2> = seasonBests.map {
                            RunnerWorkoutPlanDTOV2(it.runner,
                                    listOf(WorkoutComponentPlanElement(
                                            distance,
                                            component.duration,
                                            (it.seasonBest.first().time.calculateSecondsFrom() + paceAdjustment).toMinuteSecondString(),
                                            listOf(TargetedPace("perMile", ((it.seasonBest.first().time.calculateSecondsFrom() * distanceRatio + tempoScale + paceAdjustment).round(2).toMinuteSecondString())))
                                    )
                                )
                            )
                        }

                        return runnerWorkoutPlanDTOV2
                    }
                    "PR" -> {
                        val gradClass = date.getYearString().toInt().toString()

                        val prs = prService.getAllPRs(gradClass, "", SortingMethodContainer.TIME, false)

                        val runnerWorkoutPlanDTOV2: List<RunnerWorkoutPlanDTOV2> = prs.map {
                            RunnerWorkoutPlanDTOV2(it.runner,
                                    listOf(WorkoutComponentPlanElement(
                                            distance,
                                            component.duration,
                                            (it.pr.first().time.calculateSecondsFrom() + paceAdjustment).toMinuteSecondString(),
                                            listOf(TargetedPace("perMile", (it.pr.first().time.calculateSecondsFrom() * distanceRatio + tempoScale + paceAdjustment).round(2).toMinuteSecondString()))
                                    )
                                )
                            )
                        }

                        return runnerWorkoutPlanDTOV2
                    }
                    "Season Avg" -> {
                        // find all runners whose graduating class is > current year
                        val eligibleRunners = runnerRepository.findByGraduatingClassGreaterThan(date.getYearString()).map { it.id to it }.toMap()

                        val runnerWorkoutPlanDTOV2: List<RunnerWorkoutPlanDTOV2> = getSeasonAverages(eligibleRunners, startDate, endDate).map {
                            RunnerWorkoutPlanDTOV2(eligibleRunners[it.key]!!,
                                    listOf(WorkoutComponentPlanElement(
                                            distance,
                                            component.duration,
                                            (it.value.first().toMinuteSecondString().calculateSecondsFrom() + paceAdjustment).toMinuteSecondString(),
                                            listOf(TargetedPace("perMile", (distanceRatio * it.value.first() + tempoScale + paceAdjustment).toMinuteSecondString()))
                                    )
                                )
                            )
                        }

                        return runnerWorkoutPlanDTOV2
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

                        val runnerWorkoutPlanDTOV2: List<RunnerWorkoutPlanDTOV2> = seasonGoals.map {

                            val baseTimePerMile = it.goals.first().value.calculateSecondsFrom() * distanceRatio + paceAdjustment

                            RunnerWorkoutPlanDTOV2(it.runner,
                                    listOf(WorkoutComponentPlanElement(
                                            distance,
                                            component.duration,
                                            (it.goals.first().value.calculateSecondsFrom() * distanceRatio + paceAdjustment).toMinuteSecondString(),
                                            constructProgressionTargetedPaces(baseTimePerMile)
                                    )
                                )
                            )
                        }.toMutableList().sortedBy { it.componentPlans[0].baseTime }
                        return runnerWorkoutPlanDTOV2


                    }
                    "SB" -> {

                        val seasonBests = seasonBestService.getAllSeasonBests(startDate, endDate, false)

                        val runnerWorkoutPlanDTOV2: List<RunnerWorkoutPlanDTOV2> = seasonBests.map {
                            val baseTimePerMile = it.seasonBest.first().time.calculateSecondsFrom() * distanceRatio + paceAdjustment
                            RunnerWorkoutPlanDTOV2(it.runner,
                                    listOf(WorkoutComponentPlanElement(
                                            distance,
                                            component.duration,
                                            (it.seasonBest.first().time.calculateSecondsFrom() + paceAdjustment).toMinuteSecondString(),
                                            constructProgressionTargetedPaces(baseTimePerMile)
                                    )
                                )
                            )
                        }

                        return runnerWorkoutPlanDTOV2

                    }
                    "PR" -> {
                        val gradClass = date.getYearString().toInt().toString()

                        val prs = prService.getAllPRs(gradClass, "", SortingMethodContainer.TIME, false)

                        val runnerWorkoutPlanDTOV2: List<RunnerWorkoutPlanDTOV2> = prs.map {
                            val baseTimePerMile = it.pr.first().time.calculateSecondsFrom() * distanceRatio + paceAdjustment
                            RunnerWorkoutPlanDTOV2(it.runner,
                                    listOf(WorkoutComponentPlanElement(
                                            distance,
                                            component.duration,
                                            (it.pr.first().time.calculateSecondsFrom() + paceAdjustment).toMinuteSecondString(),
                                            constructProgressionTargetedPaces(baseTimePerMile)
                                    )
                                )
                            )
                        }

                        return runnerWorkoutPlanDTOV2



                    }
                    "Season Avg" -> {
                        // find all runners whose graduating class is > current year
                        val eligibleRunners = runnerRepository.findByGraduatingClassGreaterThan(date.getYearString()).map { it.id to it }.toMap()

                        val runnerWorkoutPlanDTOV2: List<RunnerWorkoutPlanDTOV2> = getSeasonAverages(eligibleRunners, startDate, endDate).map {
                            val basePacePerMile = distanceRatio * it.value.first() + paceAdjustment
                            RunnerWorkoutPlanDTOV2(eligibleRunners[it.key]!!,
                                    listOf(WorkoutComponentPlanElement(
                                            distance,
                                            component.duration,
                                            (it.value.first().toMinuteSecondString().calculateSecondsFrom() + paceAdjustment).toMinuteSecondString(),
                                            constructProgressionTargetedPaces(basePacePerMile)
                                    )
                                )
                            )
                        }

                        return runnerWorkoutPlanDTOV2
                    }
                }

            }
            "descriptionOnly" -> {
                return emptyList()
            }
            else -> {
                return emptyList()
            }
        }

        return emptyList()

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