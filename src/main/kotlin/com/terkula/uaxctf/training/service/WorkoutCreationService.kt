package com.terkula.uaxctf.training.service

import com.terkula.uaxctf.statistics.controller.MeetPerformanceController
import com.terkula.uaxctf.statistics.dto.toMinuteSecondString
import com.terkula.uaxctf.statistics.request.SortingMethodContainer
import com.terkula.uaxctf.statistics.service.PersonalRecordService
import com.terkula.uaxctf.statistics.service.SeasonBestService
import com.terkula.uaxctf.training.dto.RunnerWorkoutPlanDTO
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
                              internal var prService: PersonalRecordService) {

    fun createWorkout(type: String, distance: Int, pace: String): WorkoutCreationResponse? {

        var startDate = Date.valueOf("${MeetPerformanceController.CURRENT_YEAR}-01-01")
        var endDate = Date.valueOf((MeetPerformanceController.CURRENT_YEAR) + "-12-31")

        when (type) {
            "interval" -> {

                if (pace == "goal") {
                    return null
                } else if (pace == "seasonBest") {

                    val seasonBests = seasonBestService.getAllSeasonBests(startDate, endDate)
                    val distanceRatio = distance.toDouble() / fiveK.toDouble()

                    val workoutPlanDTOs = seasonBests.map {
                        RunnerWorkoutPlanDTO(it.runner, it.seasonBest.first().time, (it.seasonBest.first().time.calculateSecondsFrom() * (distanceRatio)).round(2).toMinuteSecondString())
                    }

                    return WorkoutCreationResponse(WorkoutCreationMetadata(type, distance, pace), workoutPlanDTOs)

                } else if (pace == "pr") {
                 val gradClass = MeetPerformanceController.CURRENT_YEAR.toInt().toString()

                    val prs = prService.getAllPRs(gradClass, "", SortingMethodContainer.TIME)
                    val distanceRatio = distance.toDouble() / fiveK.toDouble()
                    val workoutPlanDTOs = prs.map {
                        RunnerWorkoutPlanDTO(it.runner, it.pr.first().time, (it.pr.first().time.calculateSecondsFrom() * (distanceRatio)).round(2).toMinuteSecondString())
                    }.toList()

                    return WorkoutCreationResponse(WorkoutCreationMetadata(type, distance, pace), workoutPlanDTOs)


                } else if (pace == "seasonAverage") {
                    return null
                }

            }
            "tempo" -> {
                return null
            }
            "progression" -> {
                return null
            }
            else -> {
                return null
            }
        }

        return null

    }

    companion object {
        val fiveK: Int = 5000
    }

}