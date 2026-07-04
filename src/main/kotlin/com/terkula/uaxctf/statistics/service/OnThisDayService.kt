package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statistics.repository.MeetRepository
import com.terkula.uaxctf.statistics.repository.track.TrackMeetRepository
import com.terkula.uaxctf.statistics.response.OnThisDayEvent
import com.terkula.uaxctf.statistics.response.OnThisDayResponse
import com.terkula.uaxctf.statistics.response.OnThisDayYearGroup
import com.terkula.uaxctf.training.model.Firebase.TrainingEvent
import com.terkula.uaxctf.training.model.Firebase.Type
import com.terkula.uaxctf.training.model.Firebase.WorkoutEvent
import com.terkula.uaxctf.training.repository.RunnerWorkoutDistanceRepository
import com.terkula.uaxctf.training.repository.TrainingRunRepository
import com.terkula.uaxctf.training.repository.WorkoutComponentRepository
import com.terkula.uaxctf.training.repository.WorkoutRepository
import com.terkula.uaxctf.training.service.TrainingRunsService
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.ZoneId

@Service
class OnThisDayService(
    val trainingRunsService: TrainingRunsService,
    val trainingRunRepository: TrainingRunRepository,
    val workoutRepository: WorkoutRepository,
    val workoutComponentRepository: WorkoutComponentRepository,
    val runnerWorkoutDistanceRepository: RunnerWorkoutDistanceRepository,
    val meetRepository: MeetRepository,
    val trackMeetRepository: TrackMeetRepository,
    val meetLogService: MeetLogService
) {

    private val defaultZoneId: ZoneId = ZoneId.of("-05:00")
    private val startYear = 2018

    fun getOnThisDay(runnerId: Int, month: Int, day: Int): OnThisDayResponse {
        val currentYear = LocalDate.now().year
        val yearGroups = mutableListOf<OnThisDayYearGroup>()

        for (year in (currentYear - 1) downTo startYear) {
            val localDate = try {
                LocalDate.of(year, month, day)
            } catch (e: Exception) {
                // handles Feb 29 on non-leap years
                continue
            }

            val sqlDate = java.sql.Date(java.util.Date.from(localDate.atStartOfDay(defaultZoneId).toInstant()).time)
            val events = mutableListOf<OnThisDayEvent>()

            // Training runs
            val trainingRunResults = trainingRunsService.getARunnersTrainingRunsWithinDates(runnerId, sqlDate, sqlDate)
            trainingRunResults.trainingRunResults
                .map { it.results }
                .flatten()
                .filter { it.runner.id == runnerId }
                .forEach {
                    val trainingRun = trainingRunRepository.findByUuid(it.trainingRunUuid).firstOrNull() ?: return@forEach

                    val trainingEvent = TrainingEvent(
                        uuid = trainingRun.uuid,
                        title = trainingRun.name,
                        date = sqlDate,
                        time = trainingRun.time,
                        minTime = trainingRun.minTime,
                        distance = trainingRun.distance,
                        minDistance = trainingRun.minDistance,
                        season = trainingRun.season,
                        type = Type.training,
                        description = null,
                        team = trainingRun.team,
                        useTrainingPercent = trainingRun.useTrainingPercent,
                        effortLabel = trainingRun.effortLabel
                    )

                    events.add(OnThisDayEvent(
                        year = year,
                        date = sqlDate,
                        type = "training_run",
                        title = trainingRun.name,
                        description = "${it.distance}m run",
                        uuid = trainingRun.uuid,
                        icon = trainingRun.icon,
                        trainingRunEvent = trainingEvent
                    ))
                }

            // Workouts
            workoutRepository.findByDateBetween(sqlDate, sqlDate)
                .filter { runnerWorkoutDistanceRepository.findByWorkoutUuidAndRunnerId(it.uuid, runnerId).isNotEmpty() }
                .forEach {
                    val components = workoutComponentRepository.findByWorkoutUuid(it.uuid)

                    val workoutEvent = WorkoutEvent(
                        date = sqlDate,
                        title = it.title,
                        description = it.description,
                        icon = it.icon,
                        uuid = it.uuid,
                        components = components,
                        season = it.season,
                        team = it.team
                    )

                    events.add(OnThisDayEvent(
                        year = year,
                        date = sqlDate,
                        type = "workout",
                        title = it.title,
                        description = it.description,
                        uuid = it.uuid,
                        icon = it.icon,
                        workoutEvent = workoutEvent
                    ))
                }

            // XC meets
            meetRepository.findByDateBetween(sqlDate, sqlDate).forEach { meet ->
                val meetLog = meetLogService.getMeetLog(meet.uuid, runnerId)
                if (meetLog.meetLog != null) {
                    events.add(OnThisDayEvent(
                        year = year,
                        date = sqlDate,
                        type = "xc_meet",
                        title = meet.name,
                        description = "XC Meet",
                        uuid = meet.uuid,
                        icon = "🏁",
                        meetLogResponse = meetLog
                    ))
                }
            }

            // Track meets
            trackMeetRepository.findByDateBetween(sqlDate, sqlDate).forEach { trackMeet ->
                val trackMeetLog = meetLogService.getTrackMeetLogs(trackMeet.uuid, runnerId)
                if (trackMeetLog.trackMeetLogs.isNotEmpty()) {
                    events.add(OnThisDayEvent(
                        year = year,
                        date = sqlDate,
                        type = "track_meet",
                        title = trackMeet.name,
                        description = "Track Meet",
                        uuid = trackMeet.uuid,
                        icon = "🏟️",
                        trackMeetLogResponse = trackMeetLog
                    ))
                }
            }

            if (events.isNotEmpty()) {
                yearGroups.add(OnThisDayYearGroup(year = year, events = events))
            }
        }

        return OnThisDayResponse(month = month, day = day, runnerId = runnerId, years = yearGroups)
    }
}
