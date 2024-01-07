package com.terkula.uaxctf.scheduled.notifications.training

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.statistics.controller.firebase.FirebaseMessageService
import com.terkula.uaxctf.statistics.service.RunnerService
import com.terkula.uaxctf.training.model.DateRangeRunSummaryDTO
import com.terkula.uaxctf.training.repository.RunnersTrainingRunRepository
import com.terkula.uaxctf.training.repository.TrainingRunRepository
import com.terkula.uaxctf.training.service.TrainingRunsService
import com.terkula.uaxctf.util.TimeUtilities
import com.terkula.uaxctf.util.calculateSecondsFrom
import com.terkula.uaxctf.util.subtractDays
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

@Service
class ScheduledWeeklyTrainingSummaryNotificationService(
        val runnerService: RunnerService,
        val trainingRunsService: TrainingRunsService,
        val firebaseMessageService: FirebaseMessageService,
        val runnersTrainingRunRepository: RunnersTrainingRunRepository,
        val trainingRunRepository: TrainingRunRepository

) {

    @Scheduled(cron = "0 0 15 * * 0") // this is utc 15th hour, so 10am ET
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
        }.filter { it.deviceId != null }

        runners.forEach{
            getRunnersWeeklyMiles(startDateOfSeason, java.sql.Date(currentDate.time), it, seasonType, seasonYear)
        }

    }

    fun getRunnersWeeklyMiles(startDate: java.sql.Date, endDate: java.sql.Date, runner: Runner, type: String, seasonYear: String): Boolean {

        val objectMapper = ObjectMapper().registerKotlinModule()

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

        firebaseMessageService.sendMessageToDeviceId(runner.deviceId!!, title, message, mapOf())

        firebaseMessageService.sendMessageToDeviceId(FirebaseMessageService.DAVIDS_DEVICE_TOKEN, titleToCoach, messageToCoach,
                mapOf(
                        "type" to "runnersWeeklySummaryToCoach",
                        "runner" to objectMapper.writeValueAsString(runner),
                        "seasonType" to type,
                        "seasonYear" to seasonYear
                )
        )
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

            firebaseMessageService.sendMessageToDeviceId(FirebaseMessageService.DAVIDS_DEVICE_TOKEN, titleToCoach, messageToCoach,
                    mapOf()
            )
        } else {
            return
        }

    }

}