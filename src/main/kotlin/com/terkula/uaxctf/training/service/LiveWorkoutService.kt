package com.terkula.uaxctf.training.service

import com.terkula.uaxctf.statistics.controller.firebase.FirebaseMessageService
import com.terkula.uaxctf.training.dto.LiveWorkoutDTO
import com.terkula.uaxctf.training.model.liveworkout.LiveWorkout
import com.terkula.uaxctf.training.repository.LiveWorkoutRepository
import com.terkula.uaxctf.training.request.liveworkout.CreateLiveWorkoutEntryRequest
import com.terkula.uaxctf.util.removeDecimal
import com.terkula.uaxctf.util.round
import com.terkula.uaxctf.util.toMinuteSecondString
import org.springframework.stereotype.Service
import java.sql.Timestamp
import java.time.Instant
import java.util.*
import kotlin.math.roundToInt

@Service
class LiveWorkoutService(
        val liveWorkoutRepository: LiveWorkoutRepository,
        val firebaseMessageService: FirebaseMessageService
) {

    val BENTLEY_APP_USER_ID = 5
    val BECKY_APP_USER_ID = 26
    val DAVID_APP_USER_ID = 25

    fun getLatestLiveWorkout(username: String): LiveWorkoutDTO? {
        var liveWorkout: LiveWorkout? = liveWorkoutRepository.findFirstByUsernameOrderByTimestampDesc(username)
                ?: return null

        return LiveWorkoutDTO(
                liveWorkout!!.uuid,
                liveWorkout.workoutUuid,
                liveWorkout.timestamp.toString(),
                liveWorkout.username,
                liveWorkout.heartRate.roundToInt(),
                liveWorkout.avgHeartRate.roundToInt(),
                liveWorkout.activeEnergy.roundToInt(),
                liveWorkout.vo2Max,
                liveWorkout.distance,
                liveWorkout.elapsedTime.toMinuteSecondString().removeDecimal(),
                (liveWorkout.avgPace * 60).toMinuteSecondString().removeDecimal(),
                (liveWorkout.currentPace * 60.0).toMinuteSecondString().removeDecimal(),
                liveWorkout.avgPower.roundToInt(),
                liveWorkout.currentPower.roundToInt(),
                liveWorkout.predictedMarathon,
                liveWorkout.rollingPace,
                liveWorkout.songTitle,
                liveWorkout.songArtist
        )

    }

    fun createLiveWorkoutEntry(createLiveWorkoutEntryRequest: CreateLiveWorkoutEntryRequest): LiveWorkoutDTO {

        val latestLiveWorkout = getLatestLiveWorkout("David")
        if (latestLiveWorkout == null || latestLiveWorkout.workoutUuid != createLiveWorkoutEntryRequest.workoutUuid) {
            // send notification

            firebaseMessageService.sendMessageToAppUser(BENTLEY_APP_USER_ID, "David started a run!",
                    "Check out how his workout is going!",
                    mapOf("type" to "coach_david_live_workout"
                    )
            )

            firebaseMessageService.sendMessageToAppUser(BECKY_APP_USER_ID, "David started a run!",
                    "Check out how his workout is going!",
                    mapOf("type" to "coach_david_live_workout"
                    )
            )

            firebaseMessageService.sendMessageToAppUser(DAVID_APP_USER_ID, "You started a run!",
                    "Check out your stats!",
                    mapOf("type" to "coach_david_live_workout"
                    )
            )


        }

        var liveWorkout: LiveWorkout = liveWorkoutRepository.save(
                LiveWorkout(
                        UUID.randomUUID().toString(),
                        createLiveWorkoutEntryRequest.workoutUuid,
                        Timestamp.from(Instant.now()),
                        createLiveWorkoutEntryRequest.username,
                        createLiveWorkoutEntryRequest.heartRate.round(2),
                        createLiveWorkoutEntryRequest.avgHeartRate.round(2),
                        createLiveWorkoutEntryRequest.activeEnergy.round(2),
                        createLiveWorkoutEntryRequest.vo2Max.round(2),
                        createLiveWorkoutEntryRequest.distance.round(2),
                        createLiveWorkoutEntryRequest.elapsedTime.round(2),
                        createLiveWorkoutEntryRequest.avgPace.round(2),
                        createLiveWorkoutEntryRequest.currentPace.round(2),
                        createLiveWorkoutEntryRequest.avgPower.round(2),
                        createLiveWorkoutEntryRequest.currentPower.round(2),
                        createLiveWorkoutEntryRequest.predictedMarathonFinishTime,
                        createLiveWorkoutEntryRequest.rollingMilePace,
                        createLiveWorkoutEntryRequest.songTitle,
                        createLiveWorkoutEntryRequest.songArtist
                )
        )

        return LiveWorkoutDTO(
                liveWorkout!!.uuid,
                liveWorkout.workoutUuid,
                liveWorkout.timestamp.toString(),
                liveWorkout.username,
                liveWorkout.heartRate.roundToInt(),
                liveWorkout.avgHeartRate.roundToInt(),
                liveWorkout.activeEnergy.roundToInt(),
                liveWorkout.vo2Max,
                liveWorkout.distance,
                liveWorkout.elapsedTime.toMinuteSecondString().removeDecimal(),
                (liveWorkout.avgPace * 60).toMinuteSecondString().removeDecimal(),
                (liveWorkout.currentPace * 60.0).toMinuteSecondString().removeDecimal(),
                liveWorkout.avgPower.roundToInt(),
                liveWorkout.currentPower.roundToInt(),
                liveWorkout.predictedMarathon,
                liveWorkout.rollingPace,
                liveWorkout.songTitle,
                liveWorkout.songArtist
        )
    }

}