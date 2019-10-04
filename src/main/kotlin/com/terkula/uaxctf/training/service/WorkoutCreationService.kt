package com.terkula.uaxctf.training.service

import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.statisitcs.model.toMeetPerformanceDTO
import com.terkula.uaxctf.statistics.controller.MeetPerformanceController
import com.terkula.uaxctf.statistics.dto.toMinuteSecondString
import com.terkula.uaxctf.statistics.repository.MeetPerformanceRepository
import com.terkula.uaxctf.statistics.repository.MeetRepository
import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.statistics.request.SortingMethodContainer
import com.terkula.uaxctf.statistics.service.PersonalRecordService
import com.terkula.uaxctf.statistics.service.SeasonBestService
import com.terkula.uaxctf.statistics.service.XcGoalService
import com.terkula.uaxctf.training.dto.RunnerWorkoutPlanDTO
import com.terkula.uaxctf.training.model.TargetedPace
import com.terkula.uaxctf.training.response.WorkoutCreationMetadata
import com.terkula.uaxctf.training.response.WorkoutCreationResponse
import com.terkula.uaxctf.util.calculateSecondsFrom
import com.terkula.uaxctf.util.round
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.sql.Date

@Component
class WorkoutCreationService (@field:Autowired
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
                              internal var xcGoalService: XcGoalService) {

    fun createWorkout(type: String, distance: Int, pace: String): WorkoutCreationResponse? {

        val startDate = Date.valueOf("${MeetPerformanceController.CURRENT_YEAR}-01-01")
        val endDate = Date.valueOf((MeetPerformanceController.CURRENT_YEAR) + "-12-31")
        when (type) {
            "interval" -> {
                val distanceRatio = distance.toDouble() / fiveK.toDouble()
                when (pace) {
                    "goal" -> {

                        val seasonGoals = xcGoalService.getGoalsForSeason(MeetPerformanceController.CURRENT_YEAR)
                        val workoutPlanDTOs =  seasonGoals.map { RunnerWorkoutPlanDTO(it.runner, it.time, listOf(TargetedPace("split",
                                (it.time.calculateSecondsFrom() * distanceRatio).toMinuteSecondString()))) }.toMutableList().sortedBy { it.baseTime }

                        return WorkoutCreationResponse(WorkoutCreationMetadata(type, distance, pace), workoutPlanDTOs)


                    }
                    "seasonBest" -> {

                        val seasonBests = seasonBestService.getAllSeasonBests(startDate, endDate)

                        val workoutPlanDTOs = seasonBests.map {
                            RunnerWorkoutPlanDTO(it.runner, it.seasonBest.first().time, listOf(TargetedPace("split",
                                    (it.seasonBest.first().time.calculateSecondsFrom() * (distanceRatio)).round(2).toMinuteSecondString())))
                        }

                        return WorkoutCreationResponse(WorkoutCreationMetadata(type, distance, pace), workoutPlanDTOs)

                    }
                    "pr" -> {
                        val gradClass = MeetPerformanceController.CURRENT_YEAR.toInt().toString()

                        val prs = prService.getAllPRs(gradClass, "", SortingMethodContainer.TIME)

                        val workoutPlanDTOs = prs.map {
                            RunnerWorkoutPlanDTO(it.runner, it.pr.first().time, listOf(TargetedPace("split",
                                    (it.pr.first().time.calculateSecondsFrom() * (distanceRatio)).round(2).toMinuteSecondString())))
                        }.toList()

                        return WorkoutCreationResponse(WorkoutCreationMetadata(type, distance, pace), workoutPlanDTOs)


                    }
                    "seasonAverage" -> {

                        // find all runners whose graduating class is > current year
                        val eligibleRunners = runnerRepository.findByGraduatingClassGreaterThan(MeetPerformanceController.CURRENT_YEAR).map { it.id to it }.toMap()

                        val workoutPlans = getSeasonAverages(eligibleRunners, startDate, endDate).map {
                            RunnerWorkoutPlanDTO(eligibleRunners[it.key]!!,
                                    it.value.first().toMinuteSecondString(), listOf(TargetedPace("split", (distanceRatio * it.value.first()).toMinuteSecondString())))
                        }

                        return WorkoutCreationResponse(WorkoutCreationMetadata(type, distance, pace), workoutPlans)

                    }
                }

            }
            "tempo" -> {
                val distanceRatio = 1609.0/5000.0
                when (pace) {
                    "goal" -> {

                        val seasonGoals = xcGoalService.getGoalsForSeason(MeetPerformanceController.CURRENT_YEAR)
                        val workoutPlanDTOs =  seasonGoals.map { RunnerWorkoutPlanDTO(it.runner, it.time, listOf(TargetedPace("split",
                                (it.time.calculateSecondsFrom() * distanceRatio).toMinuteSecondString()))) }.toMutableList().sortedBy { it.baseTime }

                        return WorkoutCreationResponse(WorkoutCreationMetadata(type, distance, pace), workoutPlanDTOs)

                    }
                    "seasonBest" -> {

                        val seasonBests = seasonBestService.getAllSeasonBests(startDate, endDate)


                        val workoutPlanDTOs = seasonBests.map {
                            RunnerWorkoutPlanDTO(it.runner, it.seasonBest.first().time, listOf(TargetedPace("perMile", ((it.seasonBest.first().time
                                    .calculateSecondsFrom() * distanceRatio + tempoScale).round(2).toMinuteSecondString()))))
                        }

                        return WorkoutCreationResponse(WorkoutCreationMetadata(type, distance, pace), workoutPlanDTOs)

                    }
                    "pr" -> {
                        val gradClass = MeetPerformanceController.CURRENT_YEAR.toInt().toString()

                        val prs = prService.getAllPRs(gradClass, "", SortingMethodContainer.TIME)
                        val workoutPlanDTOs = prs.map {
                            RunnerWorkoutPlanDTO(it.runner, it.pr.first().time, listOf(TargetedPace("perMile", (it.pr.first().time.calculateSecondsFrom() * distanceRatio + tempoScale).round(2).toMinuteSecondString())))
                        }.toList()

                        return WorkoutCreationResponse(WorkoutCreationMetadata(type, distance, pace), workoutPlanDTOs)


                    }
                    "seasonAverage" -> {
                        // find all runners whose graduating class is > current year
                        val eligibleRunners = runnerRepository.findByGraduatingClassGreaterThan(MeetPerformanceController.CURRENT_YEAR).map { it.id to it }.toMap()

                        val workoutPlans = getSeasonAverages(eligibleRunners, startDate, endDate).map {
                            RunnerWorkoutPlanDTO(eligibleRunners[it.key]!!,
                                    it.value.first().toMinuteSecondString(), listOf(TargetedPace("perMile", (distanceRatio * it.value.first() + tempoScale).toMinuteSecondString())))
                        }

                        return WorkoutCreationResponse(WorkoutCreationMetadata(type, distance, pace), workoutPlans)
                    }
                }
            }
            "progression" -> {
                val distanceRatio = 1609.0/5000.0

                when (pace) {
                    "goal" -> {

                        val seasonGoals = xcGoalService.getGoalsForSeason(MeetPerformanceController.CURRENT_YEAR)
                        val workoutPlanDTOs = seasonGoals.map {
                            val baseTimePerMile = it.time.calculateSecondsFrom() * distanceRatio
                            RunnerWorkoutPlanDTO(it.runner, it.time, constructProgressionTargetedPaces(baseTimePerMile))
                        }


                        return WorkoutCreationResponse(WorkoutCreationMetadata(type, distance, pace), workoutPlanDTOs)

                    }
                    "seasonBest" -> {

                        val seasonBests = seasonBestService.getAllSeasonBests(startDate, endDate)


                        val workoutPlanDTOs = seasonBests.map {
                            val baseTimePerMile = it.seasonBest.first().time.calculateSecondsFrom() * distanceRatio
                            RunnerWorkoutPlanDTO(it.runner, it.seasonBest.first().time, constructProgressionTargetedPaces(baseTimePerMile))
                        }

                        return WorkoutCreationResponse(WorkoutCreationMetadata(type, distance, pace), workoutPlanDTOs)

                    }
                    "pr" -> {
                        val gradClass = MeetPerformanceController.CURRENT_YEAR.toInt().toString()

                        val prs = prService.getAllPRs(gradClass, "", SortingMethodContainer.TIME)
                        val workoutPlanDTOs = prs.map {
                            val baseTimePerMile = it.pr.first().time.calculateSecondsFrom() * distanceRatio
                            RunnerWorkoutPlanDTO(it.runner, it.pr.first().time, constructProgressionTargetedPaces(baseTimePerMile))
                        }.toList()

                        return WorkoutCreationResponse(WorkoutCreationMetadata(type, distance, pace), workoutPlanDTOs)


                    }
                    "seasonAverage" -> {
                        // find all runners whose graduating class is > current year
                        val eligibleRunners = runnerRepository.findByGraduatingClassGreaterThan(MeetPerformanceController.CURRENT_YEAR).map { it.id to it }.toMap()

                        val workoutPlans = getSeasonAverages(eligibleRunners, startDate, endDate) .map {
                            val basePacePerMile = distanceRatio * it.value.first()
                            RunnerWorkoutPlanDTO(eligibleRunners[it.key]!!,
                                    it.value.first().toMinuteSecondString(), constructProgressionTargetedPaces(basePacePerMile))
                        }

                        return WorkoutCreationResponse(WorkoutCreationMetadata(type, distance, pace), workoutPlans)
                    }
                }

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