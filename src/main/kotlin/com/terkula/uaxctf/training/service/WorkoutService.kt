package com.terkula.uaxctf.training.service

import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.statisitcs.model.toMeetPerformanceDTO
import com.terkula.uaxctf.statisitcs.model.track.toLogicalEvent
import com.terkula.uaxctf.statistics.dto.RunnerGoalDTO
import com.terkula.uaxctf.statistics.repository.MeetPerformanceRepository
import com.terkula.uaxctf.statistics.repository.MeetRepository
import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.statistics.request.SortingMethodContainer
import com.terkula.uaxctf.statistics.service.PersonalRecordService
import com.terkula.uaxctf.statistics.service.RunnerService
import com.terkula.uaxctf.statistics.service.SeasonBestService
import com.terkula.uaxctf.statistics.service.XcGoalService
import com.terkula.uaxctf.statistics.service.track.TrackPRService
import com.terkula.uaxctf.statistics.service.track.TrackSBService
import com.terkula.uaxctf.training.dto.ComponentToRunnerWorkoutPlans
import com.terkula.uaxctf.training.dto.RunnerWorkoutPlanDTOV2
import com.terkula.uaxctf.training.dto.WorkoutComponentPlanElement
import com.terkula.uaxctf.training.model.TargetedPace
import com.terkula.uaxctf.training.model.Workout
import com.terkula.uaxctf.training.model.WorkoutComponent
import com.terkula.uaxctf.training.model.trainingbase.TrainingBasePerformance
import com.terkula.uaxctf.training.repository.WorkoutComponentRepository
import com.terkula.uaxctf.training.repository.WorkoutRepository
import com.terkula.uaxctf.training.request.CreateWorkoutRequest
import com.terkula.uaxctf.training.response.*
import com.terkula.uaxctf.util.*
import org.springframework.stereotype.Component
import java.lang.RuntimeException
import java.sql.Date
import java.time.Month

@Component
class WorkoutService (
     var seasonBestService: SeasonBestService,
     var prService: PersonalRecordService,
     var runnerRepository: RunnerRepository,
     var meetRepository: MeetRepository,
     var meetPerformanceRepository: MeetPerformanceRepository,
     var xcGoalService: XcGoalService,
     var  workoutRepository: WorkoutRepository,
     var workoutRepositoryV2: WorkoutRepository,
     var workoutComponentRepository: WorkoutComponentRepository,
     var workoutSplitService: WorkoutSplitService,
     var runnerService: RunnerService,
     var trackPRService: TrackPRService,
     val trackSBService: TrackSBService,
     val trainingBasePerformanceService: TrainingBasePerformanceService
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
                    workoutComponentRepository.findByWorkoutUuid(it.uuid),
                    it.season,
                    it.team
            )
        }

    }

    fun createWorkoutV2(
           createWorkoutRequest: CreateWorkoutRequest
    ): WorkoutResponseDTO {

        if (workoutRepository.findByDate(createWorkoutRequest.date).firstOrNull()?.title == createWorkoutRequest.title) {
            throw RuntimeException("Workout with that date and title already exists")
        } else {
           val workoutV2 = Workout(createWorkoutRequest.date, createWorkoutRequest.description, createWorkoutRequest.title,
           createWorkoutRequest.icon, createWorkoutRequest.uuid, createWorkoutRequest.season, createWorkoutRequest.team)

           val workoutComponents: List<WorkoutComponent> = createWorkoutRequest.components.map {
               WorkoutComponent(it.uuid, workoutV2.uuid, it.type, it.description, it.targetDistance,
                       it.targetCount, it.pace, it.duration, it.targetPaceAdjustment, it.ratio, it.sets, it.recovery, it.setRecovery,
                       it.targetEvent, it.recoveryType, it.recoveryDistance, it.percent)
           }

            workoutRepositoryV2.save(workoutV2)
            workoutComponentRepository.saveAll(workoutComponents)

            return WorkoutResponseDTO(
                    workoutV2.date,
                    workoutV2.description,
                    workoutV2.title,
                    workoutV2.icon,
                    workoutV2.uuid,
                    workoutComponents,
                    workoutV2.season,
                    workoutV2.team
            )
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
            workoutToUpdate.season = createWorkoutRequest.season
            workoutToUpdate.team = createWorkoutRequest.team
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
                    component.ratio = it.ratio
                    component.targetEvent = it.targetEvent
                    component.sets = it.sets
                    component.recovery = it.recovery
                    component.setRecovery = it.setRecovery
                    component.recoveryType = it.recoveryType
                    component.recoveryDistance = it.recoveryDistance
                    component.percent = it.percent
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
                   updatedComponents.filterNotNull(),
                   workoutToUpdate.season,
                   workoutToUpdate.team
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
                    components,
                    workoutToDelete.season,
                    workoutToDelete.team
                )
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

    fun calculateTargetPace(component: WorkoutComponent, timeToAdjust: String): String {

        val distanceRatio: Double = if (component.duration != null && component.duration!!.calculateSecondsFrom() != 0.0) {
             1609.34 / component.targetEvent // if duration based, just scale to pace per mile
        } else {
            component.targetDistance.toDouble() / component.targetEvent
        }

        if (component.ratio != null && component.ratio!! != 1.0) {
            return (timeToAdjust.calculateSecondsFrom() * component.ratio!! * distanceRatio).round(2).toMinuteSecondString()
        } else if (component.percent != null && component.percent != 100) {
            val adjustmentRatio: Double = if (component.percent!! > 100) {
                // percent 105: (100 + (100 - 105)) / 100 = 95/100 = .95
                (100.0 + (100 - component.percent!!)) / 100
            } else {
                // percent 95: (100 - ( 95-100)) / 100 = 1.05
                (100.0 - (component.percent!! - 100)) / 100
            }

            return (timeToAdjust.calculateSecondsFrom() * distanceRatio * adjustmentRatio).round(2).toMinuteSecondString()
        } else if (component.targetPaceAdjustment != "00:00" && component.targetPaceAdjustment.isNotEmpty()) {
            var paceAdjustment: Int = if (component.targetPaceAdjustment.isEmpty()) {
                0
            } else {
                component.targetPaceAdjustment.calculateSecondsFrom().toInt()
            }

            return  ((timeToAdjust.calculateSecondsFrom() + paceAdjustment) * distanceRatio).round(2).toMinuteSecondString()
        } else {
            return (timeToAdjust.calculateSecondsFrom() * distanceRatio).round(2).toMinuteSecondString()
        }

    }

    fun calculateTargetPaceFromBaseTrainingPace(component: WorkoutComponent, baseTrainingPerformance: TrainingBasePerformance): String {


        if ((component.duration != null && component.duration!!.calculateSecondsFrom() != 0.0) || component.type.equals("Tempo", true)) {

            val secondsPerMile = baseTrainingPerformance.seconds / baseTrainingPerformance.fractionOfMiles

            return if (component.percent == null || component.percent == 100) {
                secondsPerMile.toMinuteSecondString()
            } else {
                secondsPerMile.secondsPerMileToPercentPacePerMile(component.percent!!)
            }
        } else {
            val distanceRatio = component.targetDistance.toDouble() / 1609.34

            return (baseTrainingPerformance.seconds.toDouble() / baseTrainingPerformance.fractionOfMiles).secondsPerMileToPercentPacePerMile(component.percent!!, distanceRatio)
        }
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

        var distance = component.targetDistance
        var duration = component.duration
        var startDate = Date.valueOf("${date.getYearString()}-01-01")
        var endDate = Date.valueOf((date.getYearString()) + "-12-31")

        when (type) {
            "Interval" -> {
                when (pace) {
                    "Goal" -> {
                          val seasonGoals =
                                  if (component.targetEvent == 5000) {
                                        xcGoalService.getGoalsForSeason(date.getYearString(), xcOnly = true, null)
                                    } else {
                                        val trackSeason = if (date.toLocalDate().month.value >= 10) {
                                            (date.getYearString().toInt() + 1).toString()
                                        } else {
                                            date.getYearString()
                                        }
                                        xcGoalService.getGoalsForSeason(trackSeason, xcOnly = null, true)
                                    }
                                    .map {
                                        RunnerGoalDTO(it.runner, it.goals.filter{ goal-> goal.type.equals("time", ignoreCase = true) && goal.event == component.targetEvent.toLogicalEvent() }.sortedBy{goal->goal.value})
                                    }
                                    .filter {
                                        it.goals.isNotEmpty()
                                    }

                        val runnerWorkoutPlanDTOV2: List<RunnerWorkoutPlanDTOV2> =
                                seasonGoals.map {
                                    var time = calculateTargetPace(component, (it.goals.first().value))
                                    if (component.duration != null && component.duration!!.calculateSecondsFrom() != 0.0) {

                                    }
                            RunnerWorkoutPlanDTOV2(it.runner,
                                    listOf(WorkoutComponentPlanElement(
                                            distance,
                                            component.duration,
                                            it.goals.first().value,
                                            listOf(TargetedPace("split", time)),
                                            workoutSplitService.getSplitsForRunnerAndComponent(it.runner.id, component.uuid)
                                        )
                                    )
                            )
                        }.toMutableList().sortedBy { it.componentPlans[0].baseTime }
                        return runnerWorkoutPlanDTOV2

                    }
                    "SB" -> {

                        if (component.targetEvent == 5000) {

                            if (date.toLocalDate().month.value < 6) {
                                // in track season
                                startDate = Date.valueOf("${date.getYearString()}-01-01")
                                endDate = Date.valueOf((date.getYearString().toInt()-1).toString() + "-12-31")
                            }

                            val seasonBests = seasonBestService.getSeasonBestTimeOrTrout(startDate, endDate, false)

                            val runnerWorkoutPlanDTOV2: List<RunnerWorkoutPlanDTOV2> = seasonBests.map {
                                RunnerWorkoutPlanDTOV2(it.first,
                                        listOf(WorkoutComponentPlanElement(
                                                distance,
                                                component.duration,
                                                it.second,
                                                listOf(TargetedPace("split", calculateTargetPace(component, it.second))),
                                                workoutSplitService.getSplitsForRunnerAndComponent(it.first.id, component.uuid)

                                        )
                                        )
                                )
                            }

                            return runnerWorkoutPlanDTOV2
                        } else {
                            val seasonBests = trackSBService.getAllSBs(true, component.targetEvent.toString(), workout.date.getYearString())
                                    .filter {
                                        it.bestResults.isNotEmpty()
                                    }


                            val runnerWorkoutPlanDTOV2: List<RunnerWorkoutPlanDTOV2> = seasonBests.map {
                                RunnerWorkoutPlanDTOV2(
                                        it.runner,
                                        listOf(WorkoutComponentPlanElement(
                                                distance,
                                                component.duration,
                                                (it.bestResults.first().best.time.calculateSecondsFrom() + paceAdjustment).toMinuteSecondString(),
                                                listOf(TargetedPace("split", calculateTargetPace(component, (it.bestResults.first().best.time)))),
                                                workoutSplitService.getSplitsForRunnerAndComponent(it.runner.id, component.uuid)
                                        ))
                                )
                            }

                            return runnerWorkoutPlanDTOV2

                        }

                    }
                    "PR" -> {
                        if (component.targetEvent == 5000) {



                            var gradClass = date.getYearString().toInt().toString()

                            // workout in track season, us previous year
                            if (date.toLocalDate().month.value < 6) {
                                gradClass = (date.getYearString().toInt() - 1).toString()
                            }

                            val prs = prService.getAllPRs(gradClass, "", SortingMethodContainer.TIME, false)

                            val runnerWorkoutPlanDTOV2: List<RunnerWorkoutPlanDTOV2> = prs.map {
                                RunnerWorkoutPlanDTOV2(it.runner,
                                        listOf(WorkoutComponentPlanElement(
                                                distance,
                                                component.duration,
                                                it.pr.first().time,
                                                listOf(TargetedPace("split", calculateTargetPace(component, it.pr.first().time))),
                                                workoutSplitService.getSplitsForRunnerAndComponent(it.runner.id, component.uuid)
                                        )
                                        )
                                )
                            }

                            return runnerWorkoutPlanDTOV2
                        } else {
                            val prs = trackPRService.getAllPRsForWorkoutPlan(workout.date.getYearString(), includeSplits = true, component.targetEvent.toString())
                                    .filter {
                                        it.bestResults.isNotEmpty()
                                    }

                            // this leaves gaps in workout plan for those without Prs

                            //todo handle pace adjustment by ratio if present
                            val runnerWorkoutPlanDTOV2: List<RunnerWorkoutPlanDTOV2> = prs.map {
                                RunnerWorkoutPlanDTOV2(
                                        it.runner,
                                        listOf(WorkoutComponentPlanElement(
                                                distance,
                                                component.duration,
                                                it.bestResults.first().best.time,
                                                listOf(TargetedPace("split", calculateTargetPace(component, (it.bestResults.first().best.time)))),
                                                workoutSplitService.getSplitsForRunnerAndComponent(it.runner.id, component.uuid)
                                        ))
                                )
                            }
                            return runnerWorkoutPlanDTOV2
                        }
                    }
                    "Season Avg" -> {

                        if (component.targetEvent == 5000) {
                            // find all runners whose graduating class is > current year
                            val eligibleRunners = runnerService.getRoster(true, workout.date.getYearString()).map { it.id to it }.toMap()

                            val runnerWorkoutPlanDTOV2: List<RunnerWorkoutPlanDTOV2> = getSeasonAverages(eligibleRunners, startDate, endDate).map {
                                RunnerWorkoutPlanDTOV2(eligibleRunners[it.key]!!,
                                        listOf(WorkoutComponentPlanElement(
                                                distance,
                                                component.duration,
                                                it.value.first().toMinuteSecondString(),
                                                listOf(TargetedPace("split", calculateTargetPace(component, it.value.first().toMinuteSecondString()))),
                                                workoutSplitService.getSplitsForRunnerAndComponent(eligibleRunners[it.key]!!.id, component.uuid)
                                        )
                                        )
                                )
                            }

                            return runnerWorkoutPlanDTOV2
                        } else {
                            val trackSeason = if (date.toLocalDate().month.value >= 10) {
                                (date.getYearString().toInt() + 1).toString()
                            } else {
                                date.getYearString()
                            }
                            val eligibleRunners = runnerService.getTrackRoster(true, trackSeason).map { it.id to it }.toMap()

                            val runnerWorkoutPlanDTOV2: List<RunnerWorkoutPlanDTOV2> = trackSBService.getSeasonAverages(eligibleRunners, component.targetEvent.toLogicalEvent(),
                                    TimeUtilities.getFirstDayOfGivenYear(trackSeason), TimeUtilities.getLastDayOfGivenYear(trackSeason)).map {
                                RunnerWorkoutPlanDTOV2(eligibleRunners[it.key]!!,
                                        listOf(WorkoutComponentPlanElement(
                                                distance,
                                                component.duration,
                                                it.value.first().toMinuteSecondString(),
                                                listOf(TargetedPace("split", calculateTargetPace(component, it.value.first().toMinuteSecondString()))),
                                                workoutSplitService.getSplitsForRunnerAndComponent(eligibleRunners[it.key]!!.id, component.uuid)
                                        )
                                        )
                                )
                            }
                            return runnerWorkoutPlanDTOV2
                        }


                    }
                    "Base Training Pace" -> {

                        // find all runners whose graduating class is > current year

                        val runnerWorkoutPlanDTOV2: List<RunnerWorkoutPlanDTOV2> = getBaseTrainingPaces(workout, component.targetEvent.toString()).map {
                            RunnerWorkoutPlanDTOV2(it.key,
                                    listOf(WorkoutComponentPlanElement(
                                            distance,
                                            component.duration,
                                            it.value.seconds.toDouble().toMinuteSecondString(),
                                            listOf(TargetedPace("split", calculateTargetPaceFromBaseTrainingPace(component, it.value))),
                                            workoutSplitService.getSplitsForRunnerAndComponent(it.key!!.id, component.uuid)
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

                        var yearString = date.getYearString()

                        if (date.toLocalDate().monthValue < 6) {
                            yearString = (date.getYearString().toInt() -1).toString()
                        }

                        val seasonGoals = xcGoalService.getGoalsForSeason(yearString, xcOnly = true, trackOnly = null)
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
                                            listOf(TargetedPace("split", (it.goals.first().value.calculateSecondsFrom() * distanceRatio + tempoScale + paceAdjustment).toMinuteSecondString())),
                                            workoutSplitService.getSplitsForRunnerAndComponent(it.runner.id, component.uuid)
                                    )
                                )
                            )
                        }.toMutableList().sortedBy { it.componentPlans[0].baseTime }
                        return runnerWorkoutPlanDTOV2

                    }
                    "SB" -> {

                        if (date.toLocalDate().month.value < 6) {
                            // in track season
                            startDate = Date.valueOf("${date.getYearString()}-01-01")
                            endDate = Date.valueOf((date.getYearString().toInt()-1).toString() + "-12-31")
                        }

                        val seasonBests = seasonBestService.getSeasonBestTimeOrTrout(startDate, endDate, false)

                        val runnerWorkoutPlanDTOV2: List<RunnerWorkoutPlanDTOV2> = seasonBests.map {
                            RunnerWorkoutPlanDTOV2(it.first,
                                    listOf(WorkoutComponentPlanElement(
                                            distance,
                                            component.duration,
                                            (it.second.calculateSecondsFrom() + paceAdjustment).toMinuteSecondString(),
                                            listOf(TargetedPace("perMile", ((it.second.calculateSecondsFrom() * distanceRatio + tempoScale + paceAdjustment).round(2).toMinuteSecondString()))),
                                            workoutSplitService.getSplitsForRunnerAndComponent(it.first.id, component.uuid)
                                    )
                                )
                            )
                        }

                        return runnerWorkoutPlanDTOV2
                    }
                    "PR" -> {
                        var gradClass = date.getYearString().toInt().toString()

                        // workout in track season, us previous year
                        if (date.toLocalDate().month.value < 6) {
                            gradClass = (date.getYearString().toInt() - 1).toString()
                        }

                        val prs = prService.getAllPRs(gradClass, "", SortingMethodContainer.TIME, false)

                        val runnerWorkoutPlanDTOV2: List<RunnerWorkoutPlanDTOV2> = prs.map {
                            RunnerWorkoutPlanDTOV2(it.runner,
                                    listOf(WorkoutComponentPlanElement(
                                            distance,
                                            component.duration,
                                            (it.pr.first().time.calculateSecondsFrom() + paceAdjustment).toMinuteSecondString(),
                                            listOf(TargetedPace("perMile", (it.pr.first().time.calculateSecondsFrom() * distanceRatio + tempoScale + paceAdjustment).round(2).toMinuteSecondString())),
                                            workoutSplitService.getSplitsForRunnerAndComponent(it.runner.id, component.uuid)
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
                                            listOf(TargetedPace("perMile", (distanceRatio * it.value.first() + tempoScale + paceAdjustment).toMinuteSecondString())),
                                            workoutSplitService.getSplitsForRunnerAndComponent(eligibleRunners[it.key]!!.id, component.uuid)
                                    )
                                )
                            )
                        }

                        return runnerWorkoutPlanDTOV2
                    }
                    "Base Training Pace" -> {

                        // find all runners whose graduating class is > current year

                        val runnerWorkoutPlanDTOV2: List<RunnerWorkoutPlanDTOV2> = getBaseTrainingPaces(workout, component.targetEvent.toString()).map {
                            RunnerWorkoutPlanDTOV2(it.key,
                                    listOf(WorkoutComponentPlanElement(
                                            distance,
                                            component.duration,
                                            it.value.seconds.toDouble().toMinuteSecondString(),
                                            listOf(TargetedPace("perMile", calculateTargetPaceFromBaseTrainingPace(component, it.value))),
                                            workoutSplitService.getSplitsForRunnerAndComponent(it.key!!.id, component.uuid)
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

                        val seasonGoals = xcGoalService.getGoalsForSeason(date.getYearString(), xcOnly = true, trackOnly = null)
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
                                            constructProgressionTargetedPaces(baseTimePerMile),
                                            workoutSplitService.getSplitsForRunnerAndComponent(it.runner.id, component.uuid)
                                    )
                                )
                            )
                        }.toMutableList().sortedBy { it.componentPlans[0].baseTime }
                        return runnerWorkoutPlanDTOV2


                    }
                    "SB" -> {

                        val seasonBests = seasonBestService.getSeasonBestTimeOrTrout(startDate, endDate, false)

                        val runnerWorkoutPlanDTOV2: List<RunnerWorkoutPlanDTOV2> = seasonBests.map {
                            val baseTimePerMile = it.second.calculateSecondsFrom() * distanceRatio + paceAdjustment
                            RunnerWorkoutPlanDTOV2(it.first,
                                    listOf(WorkoutComponentPlanElement(
                                            distance,
                                            component.duration,
                                            (it.second.calculateSecondsFrom() + paceAdjustment).toMinuteSecondString(),
                                            constructProgressionTargetedPaces(baseTimePerMile),
                                            workoutSplitService.getSplitsForRunnerAndComponent(it.first.id, component.uuid)
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
                                            constructProgressionTargetedPaces(baseTimePerMile),
                                            workoutSplitService.getSplitsForRunnerAndComponent(it.runner.id, component.uuid)
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
                                            constructProgressionTargetedPaces(basePacePerMile),
                                            workoutSplitService.getSplitsForRunnerAndComponent(eligibleRunners[it.key]!!.id, component.uuid)
                                    )
                                )
                            )
                        }

                        return runnerWorkoutPlanDTOV2
                    }
                    "Base Training Pace" -> {

                        // find all runners whose graduating class is > current year

                        val runnerWorkoutPlanDTOV2: List<RunnerWorkoutPlanDTOV2> = getBaseTrainingPaces(workout, component.targetEvent.toString()).map {
                            val basePacePerMile = distanceRatio * it.value.seconds + paceAdjustment
                            RunnerWorkoutPlanDTOV2(it.key,
                                    listOf(WorkoutComponentPlanElement(
                                            distance,
                                            component.duration,
                                            it.value.seconds.toDouble().toMinuteSecondString(),
                                            constructProgressionTargetedPaces(basePacePerMile.secondsPerMileToPercentPacePerMile(component.percent!!).calculateSecondsFrom()),
                                            workoutSplitService.getSplitsForRunnerAndComponent(it.key!!.id, component.uuid)
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

    fun getBaseTrainingPaces(workout: Workout, targetEvent: String): Map<Runner, TrainingBasePerformance> {

        var year = workout.date.getYearString()
        val eligibleRunners = if (workout.season == "xc") {
            runnerService.getXcRoster(true, workout.date.getYearString()).map { it.id to it }.toMap()
        } else {
            if (workout.date.toLocalDate().month == Month.NOVEMBER || workout.date.toLocalDate().month == Month.DECEMBER) {
                year = (workout.date.getYearString().toInt() + 1).toString()
            }
            runnerService.getTrackRoster(true, year).map { it.id to it }.toMap()
        }

        return trainingBasePerformanceService.getBaseTrainingPerformancesForEvent(targetEvent + "m", workout.season, year)
                .map { eligibleRunners[it.runner.id] to it.trainingBasePerformance }
                .filter { it.first != null && it.second != null }
                .map { it.first!! to it.second!! }
                .sortedBy { it.second.seconds }
                .toMap()
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
        const val tempoScale = 30
    }

}