package com.terkula.uaxctf.scheduled.notifications.training

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.statistics.controller.firebase.FirebaseMessageService
import com.terkula.uaxctf.statistics.controller.firebase.FirebaseMessageService.Companion.BENTLEYS_RUNNER_ID
import com.terkula.uaxctf.statistics.repository.AuthenticationRepository
import com.terkula.uaxctf.statistics.repository.MeetRepository
import com.terkula.uaxctf.statistics.repository.track.TrackMeetRepository
import com.terkula.uaxctf.statistics.service.MeetLogService
import com.terkula.uaxctf.statistics.service.RunnerService
import com.terkula.uaxctf.training.model.DateRangeRunSummaryDTO
import com.terkula.uaxctf.training.model.Firebase.TrainingEvent
import com.terkula.uaxctf.training.model.Firebase.Type
import com.terkula.uaxctf.training.model.Firebase.WorkoutEvent
import com.terkula.uaxctf.training.repository.RunnersTrainingRunRepository
import com.terkula.uaxctf.training.repository.TrainingRunRepository
import com.terkula.uaxctf.training.repository.WorkoutComponentRepository
import com.terkula.uaxctf.training.repository.WorkoutRepository
import com.terkula.uaxctf.training.service.TrainingRunsService
import com.terkula.uaxctf.training.service.WorkoutSplitService
import com.terkula.uaxctf.util.TimeUtilities
import com.terkula.uaxctf.util.calculateSecondsFrom
import com.terkula.uaxctf.util.subtractDays
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId
import java.util.*


@Service
class ScheduledWeeklyTrainingSummaryNotificationService(
        val runnerService: RunnerService,
        val trainingRunsService: TrainingRunsService,
        val firebaseMessageService: FirebaseMessageService,
        val runnersTrainingRunRepository: RunnersTrainingRunRepository,
        val trainingRunRepository: TrainingRunRepository,
        val workoutRepository: WorkoutRepository,
        val workoutComponentRepository: WorkoutComponentRepository,
        val trackMeetRepository: TrackMeetRepository,
        val meetRepository: MeetRepository,
        val meetLogService: MeetLogService,
        val authenticationRepository: AuthenticationRepository
) {

    //@Scheduled(cron = "0 * * * * *") // testing every minute
    @Scheduled(cron = "0 0 15 * * *") // this is utc 16th hour, so 10-11am ET
    fun sendBentleyTimeMachineNotification() {

        val objectMapper = ObjectMapper()

        val runner = runnerService.runnerRepository.findById(BENTLEYS_RUNNER_ID).get()
        val davidsDeviceId = authenticationRepository.findById(25).get().deviceId!!
        val runnerJson = objectMapper.writeValueAsString(runner)



        val defaultZoneId: ZoneId = ZoneId.of("-05:00")
        val localDate = LocalDate.now()
        val oneYearAgo = localDate.minusYears(1)

        val startDate = java.sql.Date(Date.from(oneYearAgo.atStartOfDay(defaultZoneId).toInstant()).time)
        val endDate = java.sql.Date(Date.from(oneYearAgo.atStartOfDay(defaultZoneId).toInstant()).time)

        val trainingRunLogs = trainingRunsService.getARunnersTrainingRunsWithinDates(BENTLEYS_RUNNER_ID, startDate, endDate).trainingRunResults
                .map { it.results }
                .flatten()
                .filter { it.runner.id == BENTLEYS_RUNNER_ID }

        if (trainingRunLogs.isNotEmpty()) {
            trainingRunLogs.forEach {

                val trainingRun = trainingRunRepository.findByUuid(it.trainingRunUuid).first()

                val trainingRunEvent: TrainingEvent = TrainingEvent(uuid = trainingRun.uuid, title = trainingRun.name, date = Date((startDate.time/1000)),
                time = trainingRun.time, minTime = trainingRun.minTime, distance = trainingRun.distance, minDistance =
                trainingRun.minDistance, season = trainingRun.season, type = Type.training, description = null, team = trainingRun.team, useTrainingPercent = trainingRun.useTrainingPercent,
                effortLabel = trainingRun.effortLabel)

                var trainingRunEventJson = objectMapper.writeValueAsString(trainingRunEvent)
                trainingRunEventJson = trainingRunEventJson.replace("\"training\"", "{\"training\":{}}")


                firebaseMessageService.sendMessageToDeviceId(runner.deviceId!!, "UAXCTF Time Machine",
                        "One year ago today, you did a " + it.distance + "m training run",
                        mapOf("type" to "coaches_note_training_run",
                              "runner" to runnerJson,
                              "trainingRunEvent" to trainingRunEventJson
                        )
                )

                firebaseMessageService.sendMessageToDeviceId(davidsDeviceId, "UAXCTF Time Machine",
                        "One year ago today, Bentley did a " + it.distance + "m training run",
                        mapOf("type" to "coaches_note_training_run",
                                "runner" to runnerJson,
                                "trainingRunEvent" to trainingRunEventJson
                        )

                )
            }

        }


        workoutRepository.findByDateBetween(startDate, endDate)
                .forEach {
                    val components = workoutComponentRepository.findByWorkoutUuid(it.uuid)

                    var workoutEvent: WorkoutEvent = WorkoutEvent(date = Date((startDate.time/1000)), title = it.title,
                            description = it.description, icon = it.icon, uuid = it.uuid, components = components, season = it.season, team = it.team)

                    var workoutJson = objectMapper.writeValueAsString(workoutEvent)

                    firebaseMessageService.sendMessageToDeviceId(runner.deviceId!!, "UAXCTF Time Machine",
                        "One year ago today, you did the workout: " + it.title,
                        mapOf("type" to "coaches_note_workout",
                              "runner" to runnerJson,
                              "workout" to workoutJson
                        )
                )

                    firebaseMessageService.sendMessageToDeviceId(davidsDeviceId, "UAXCTF Time Machine",
                            "One year ago today, Bentley did the workout: " + it.title,
                            mapOf("type" to "coaches_note_workout",
                                    "runner" to runnerJson,
                                    "workout" to workoutJson
                            )

                    )

                }

        val xcMeet = meetRepository.findByDateBetween(startDate, endDate).firstOrNull()
        val trackMeet = trackMeetRepository.findByDateBetween(startDate, endDate).firstOrNull()

        if (xcMeet != null) {
            val xcMeetLog = meetLogService.getMeetLog(xcMeet.uuid, BENTLEYS_RUNNER_ID)
            xcMeetLog.meet!!.date = java.sql.Date(Date.from(oneYearAgo.atStartOfDay(defaultZoneId).toInstant()).time/1000)


            if (xcMeetLog.meetLog != null) {
                var meetLogJson = objectMapper.writeValueAsString(xcMeetLog)

                firebaseMessageService.sendMessageToDeviceId(runner.deviceId!!, "UAXCTF Time Machine",
                        "One year ago today, you raced at: " + xcMeet.name + "!",
                        mapOf("type" to "coaches_note_xc_meet_log",
                                "runner" to runnerJson,
                                "xcMeetLogResponse" to meetLogJson
                        )
                )

                firebaseMessageService.sendMessageToDeviceId(davidsDeviceId, "UAXCTF Time Machine",
                        "One year ago today, Bentley raced at: " + xcMeet.name + "!",
                        mapOf("type" to "coaches_note_xc_meet_log",
                                "runner" to runnerJson,
                                "xcMeetLogResponse" to meetLogJson
                        )
                )
            }

        }

        if (trackMeet != null) {
            var meetLog = meetLogService.getMeetLog(trackMeet.uuid, BENTLEYS_RUNNER_ID)
        }

    }

     @Scheduled(cron = "0 0 14 * * 0") // this is utc 15th hour, so 10am ET
    //@Scheduled(cron = "0 * * * * *") // testing every minute
    fun sendWeeklyTrainingSummaryNotifications() {

        val defaultZoneId: ZoneId = ZoneId.of("-05:00")
        val localDate = LocalDate.now()

        val seasonType = if (localDate.month.value <= 6) {
            if (localDate.month.value == 6 && localDate.dayOfMonth >= 5) {
                "xc"
            } else {
                "track"
            }

        } else {
            if (localDate.month.value >= 11) {
                if (localDate.month.value == 11 && localDate.dayOfMonth < 6) {
                    "xc"
                } else {
                    "track"
                }
            } else {
                "xc"
            }
        }

        val startDateOfSeason = if (seasonType == "xc") {
            TimeUtilities.getFirstDayOfGivenYear(localDate.year.toString())
        } else {
            if (seasonType == "track" && localDate.month.value >= 10) {
                TimeUtilities.getLastDayOfGivenYear(localDate.year.toString()).subtractDays(90)
            } else {
                TimeUtilities.getLastDayOfGivenYear((localDate.year -1).toString()).subtractDays(90)
            }
        }


        val currentDate: Date = Date.from(localDate.atStartOfDay(defaultZoneId).toInstant())
        var seasonYear: String = localDate.year.toString()

        if (seasonType == "track" && localDate.month.value >= 10) {
            seasonYear = (localDate.year + 1).toString()
        }

        val runners = if (seasonType == "xc") {
            runnerService.getXcRoster(true, localDate.year.toString())
        } else {
            var trackYear = localDate.year.toString()

            if (localDate.month.value >= 10) {
                trackYear = (trackYear.toInt() + 1).toString()
            }
            runnerService.getTrackRoster(true, trackYear)
        }.filter { it.deviceId != null && it.deviceId!!.isNotEmpty() }

        runners.forEach{
            getRunnersWeeklyMiles(startDateOfSeason, java.sql.Date(currentDate.time), it, seasonType, seasonYear)
        }

    }

    fun getRunnersWeeklyMiles(startDate: java.sql.Date, endDate: java.sql.Date, runner: Runner, type: String, seasonYear: String): Boolean {

        val objectMapper = ObjectMapper().registerKotlinModule()

        val davidsDeviceId = authenticationRepository.findById(25).get().deviceId!!

        val runnersSeasonWeeklyMiles: List<DateRangeRunSummaryDTO> =
                trainingRunsService.getTotalDistancePerWeek(startDate, endDate, runner.id, true, type)
                        .filter { it.totalCount > 0 }

        if (runnersSeasonWeeklyMiles.isEmpty()) {
            return false
        }

        if (runnersSeasonWeeklyMiles.last().totalDistance == 0.0) {
            return false
        }

        if (runnersSeasonWeeklyMiles.last().totalDistance < 10) {
            return false
        }

        val title = "Weekly Training Summary"
        var message = ""

        var titleToCoach = runner.name + " " + title
        var messageToCoach = ""

        if (runnersSeasonWeeklyMiles.last().totalDistance == runnersSeasonWeeklyMiles.map { it.totalDistance }.maxOrNull()) {
            message = "You ran " + runnersSeasonWeeklyMiles.last().totalDistance + " miles last week," +
                    " which is your most in a week so far this season! Way to go " + runner.name.split(" ")[0] + "!"

            messageToCoach = runner.name.split(" ")[0] + " ran " + runnersSeasonWeeklyMiles.last().totalDistance + " miles last week," +
                    " which is their most in a week so far this season."
        } else if (runnersSeasonWeeklyMiles.last().trainingAvgPace.calculateSecondsFrom() == runnersSeasonWeeklyMiles.map { it.trainingAvgPace.calculateSecondsFrom() }.minOrNull()) {
            message = "You ran " + runnersSeasonWeeklyMiles.last().totalDistance + " miles last week," +
                    " at an average pace of " + runnersSeasonWeeklyMiles.last().trainingAvgPace + "s per mile." +
            " This was your fastest weekly average pace so far this season! Great job " + runner.name.split(" ")[0] + "!"

            messageToCoach = runner.name.split(" ")[0] + " ran " + runnersSeasonWeeklyMiles.last().totalDistance + " miles last week," +
                    " at an average pace of " + runnersSeasonWeeklyMiles.last().trainingAvgPace + "s per mile." +
            " This was their fastest weekly average pace so far this season."
        }
        else {
            message = "You ran " + runnersSeasonWeeklyMiles.last().totalDistance + " miles last week," +
                    " at an average pace of " + runnersSeasonWeeklyMiles.last().trainingAvgPace + "s per mile." +
             " Good work " + runner.name.split(" ")[0] + "!"

            messageToCoach = runner.name.split(" ")[0] + " ran " + runnersSeasonWeeklyMiles.last().totalDistance + " miles last week," +
                    " at an average pace of " + runnersSeasonWeeklyMiles.last().trainingAvgPace + "s per mile."
        }

        try {
            firebaseMessageService.sendMessageToDeviceId(runner.deviceId!!, title, message, mapOf())

            firebaseMessageService.sendMessageToDeviceId(davidsDeviceId, titleToCoach, messageToCoach,
                    mapOf(
                            "type" to "runnersWeeklySummaryToCoach",
                            "runner" to objectMapper.writeValueAsString(runner),
                            "seasonType" to type,
                            "seasonYear" to seasonYear
                    )
            )
        } catch (e: Exception) {
            println(e.message)
        }


        return true
    }

    @Scheduled(cron = "0 30 2 * * *") // this is utc hour, so 8 or 9pm.
    fun sendLongestRunNotification() {

        val defaultZoneId: ZoneId = ZoneId.of("-05:00")
        val localDate = LocalDate.now()
        val currentSQLDate = java.sql.Date(Date.from(localDate.atStartOfDay(defaultZoneId).toInstant()).time)

        val seasonType = if (localDate.month.value <= 6) {
            if (localDate.month.value == 6 && localDate.dayOfMonth >= 5) {
                "xc"
            } else {
                "track"
            }

        } else {
            if (localDate.month.value >= 11) {
                if (localDate.month.value == 11 && localDate.dayOfMonth < 6) {
                    "xc"
                } else {
                    "track"
                }
            } else {
                "xc"
            }
        }

        val runners = if (seasonType == "xc") {
            runnerService.getXcRoster(true, localDate.year.toString())
        } else {
            var trackYear = localDate.year.toString()

            if (localDate.month.value >= 10) {
                trackYear = (trackYear.toInt() + 1).toString()
            }
            runnerService.getTrackRoster(true, trackYear)
        }.filter { it.deviceId != null }

        runners.forEach { getLongestContinuousRunForRunner(it, currentSQLDate) }

    }

    fun getLongestContinuousRunForRunner(runner: Runner, currentDate: java.sql.Date) {

        val trainingRunLogs = runnersTrainingRunRepository.findByRunnerId(runner.id).sortedByDescending { it.distance }
        val davidsDeviceId = authenticationRepository.findById(25).get().deviceId!!

        if (trainingRunLogs.isEmpty()) {
            return
        }

        val furthestTrainingRun = trainingRunLogs.first()

        val trainingRun = trainingRunRepository.findByDate(currentDate).filter { it.uuid == furthestTrainingRun.trainingRunUuid }

        if (trainingRun.isNotEmpty()) {

            val title = "Longest Recorded Run!"

            val titleToCoach = runner.name + " " + "Longest Run"

            val message = "You ran " + furthestTrainingRun.distance + " miles today," +
                    " which is your longest run UAXCTF recorded run on record! Congrats on the milestone run " + runner.name.split(" ")[0] + "!"

            val messageToCoach = runner.name.split(" ")[0] + " ran " + furthestTrainingRun.distance + " miles today," +
                    " which is their longest recorded run!"

            firebaseMessageService.sendMessageToDeviceId(runner.deviceId!!, title, message, mapOf())

            firebaseMessageService.sendMessageToDeviceId(davidsDeviceId, titleToCoach, messageToCoach,
                    mapOf()
            )
        } else {
            return
        }

    }

}