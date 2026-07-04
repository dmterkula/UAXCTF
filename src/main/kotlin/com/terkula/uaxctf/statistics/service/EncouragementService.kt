package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statisitcs.model.Encouragement
import com.terkula.uaxctf.statisitcs.model.EncouragementReaction
import com.terkula.uaxctf.statisitcs.model.EncouragementSave
import com.terkula.uaxctf.statistics.controller.firebase.FirebaseMessageService
import com.terkula.uaxctf.statistics.repository.*
import com.terkula.uaxctf.statistics.request.CreateEncouragementReactionRequest
import com.terkula.uaxctf.statistics.request.CreateEncouragementRequest
import com.terkula.uaxctf.statistics.request.SaveEncouragementRequest
import com.terkula.uaxctf.statistics.response.EncouragementDTO
import com.terkula.uaxctf.statistics.response.EncouragementListResponse
import com.terkula.uaxctf.statistics.response.EncouragementReactionDTO
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*

@Service
class EncouragementService(
    val encouragementRepository: EncouragementRepository,
    val encouragementReactionRepository: EncouragementReactionRepository,
    val encouragementSaveRepository: EncouragementSaveRepository,
    val runnerRepository: RunnerRepository,
    val authenticationRepository: AuthenticationRepository,
    val firebaseMessageService: FirebaseMessageService
) {

    private val dateFormat: SimpleDateFormat
        get() {
            val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)
            df.timeZone = TimeZone.getTimeZone("UTC")
            return df
        }

    fun getActiveEncouragementsByTeam(team: String): EncouragementListResponse {
        val now = Timestamp(System.currentTimeMillis())
        val encouragements = encouragementRepository.findByTeamAndExpiresAtGreaterThanOrderByCreatedAtDesc(team, now)
        return EncouragementListResponse(encouragements.map { toDTO(it, saved = false) })
    }

    fun getActiveEncouragementsForRunner(runnerId: Int): EncouragementListResponse {
        val now = Timestamp(System.currentTimeMillis())
        val directed = encouragementRepository.findByRecipientRunnerIdAndExpiresAtGreaterThanOrderByCreatedAtDesc(runnerId, now)
        val teamWide = encouragementRepository.findByRecipientRunnerIdIsNullAndExpiresAtGreaterThanOrderByCreatedAtDesc(now)

        val combined = (directed + teamWide).sortedByDescending { it.createdAt }
        return EncouragementListResponse(combined.map { toDTO(it, saved = false) })
    }

    fun getSavedEncouragements(runnerId: Int): EncouragementListResponse {
        val saves = encouragementSaveRepository.findByRunnerId(runnerId)
        val encouragements = saves.mapNotNull { save ->
            encouragementRepository.findByUuid(save.encouragementUuid)
        }
        return EncouragementListResponse(encouragements.sortedByDescending { it.createdAt }.map { toDTO(it, saved = true) })
    }

    fun createEncouragement(request: CreateEncouragementRequest): EncouragementDTO {
        val recipientName = if (request.recipientRunnerId != null) {
            val runner = runnerRepository.findById(request.recipientRunnerId)
            if (runner.isPresent) runner.get().name else null
        } else {
            null
        }

        val encouragement = Encouragement(
            uuid = request.uuid,
            authorRunnerId = request.authorRunnerId,
            authorUsername = request.authorUsername,
            authorDisplayName = request.authorDisplayName,
            recipientRunnerId = request.recipientRunnerId,
            recipientName = recipientName,
            text = request.text,
            team = request.team
        )

        encouragementRepository.save(encouragement)

       // sendPushNotification(encouragement)

        return toDTO(encouragement, saved = false)
    }

    @Transactional
    fun deleteEncouragement(uuid: String) {
        encouragementReactionRepository.deleteByEncouragementUuid(uuid)
        encouragementSaveRepository.deleteByEncouragementUuid(uuid)
        encouragementRepository.deleteByUuid(uuid)
    }

    fun createReaction(request: CreateEncouragementReactionRequest): EncouragementReactionDTO {
        val reaction = EncouragementReaction(
            uuid = request.uuid,
            encouragementUuid = request.encouragementUuid,
            reactorUsername = request.reactorUsername,
            reactorDisplayName = request.reactorDisplayName,
            emoji = request.emoji
        )

        encouragementReactionRepository.save(reaction)

        val df = dateFormat
        return EncouragementReactionDTO(
            uuid = reaction.uuid,
            encouragementUuid = reaction.encouragementUuid,
            reactorUsername = reaction.reactorUsername,
            reactorDisplayName = reaction.reactorDisplayName,
            emoji = reaction.emoji,
            createdAt = df.format(reaction.createdAt)
        )
    }

    @Transactional
    fun deleteReaction(uuid: String) {
        encouragementReactionRepository.deleteByUuid(uuid)
    }

    fun saveEncouragement(request: SaveEncouragementRequest) {
        val existing = encouragementSaveRepository.findByEncouragementUuidAndRunnerId(request.encouragementUuid, request.runnerId)
        if (existing == null) {
            encouragementSaveRepository.save(EncouragementSave(request.encouragementUuid, request.runnerId))
        }
    }

    @Transactional
    fun unsaveEncouragement(encouragementUuid: String, runnerId: Int) {
        encouragementSaveRepository.deleteByEncouragementUuidAndRunnerId(encouragementUuid, runnerId)
    }

    private fun toDTO(encouragement: Encouragement, saved: Boolean): EncouragementDTO {
        val reactions = encouragementReactionRepository.findByEncouragementUuid(encouragement.uuid)
        val df = dateFormat

        return EncouragementDTO(
            uuid = encouragement.uuid,
            authorRunnerId = encouragement.authorRunnerId,
            authorUsername = encouragement.authorUsername,
            authorDisplayName = encouragement.authorDisplayName,
            recipientRunnerId = encouragement.recipientRunnerId,
            recipientName = encouragement.recipientName,
            text = encouragement.text,
            team = encouragement.team,
            saved = saved,
            reactions = reactions.map { reaction ->
                EncouragementReactionDTO(
                    uuid = reaction.uuid,
                    encouragementUuid = reaction.encouragementUuid,
                    reactorUsername = reaction.reactorUsername,
                    reactorDisplayName = reaction.reactorDisplayName,
                    emoji = reaction.emoji,
                    createdAt = df.format(reaction.createdAt)
                )
            },
            createdAt = df.format(encouragement.createdAt),
            expiresAt = df.format(encouragement.expiresAt)
        )
    }

    private fun sendPushNotification(encouragement: Encouragement) {
        val title = "New Shout Out!"
        val body = if (encouragement.recipientName != null) {
            "${encouragement.authorDisplayName} gave ${encouragement.recipientName} a shout out!"
        } else {
            "${encouragement.authorDisplayName} hyped up the team!"
        }

        val data = mapOf("type" to "encouragement_created")

        // Send to all team members with device IDs
        val runners = runnerRepository.findByTeam(encouragement.team)
            .filter { it.deviceId != null && it.deviceId!!.isNotEmpty() }

        for (runner in runners) {
            try {
                firebaseMessageService.sendMessageToDeviceId(runner.deviceId!!, title, body, data)
            } catch (e: Exception) {
                // Don't fail encouragement creation if a notification fails
                e.printStackTrace()
            }
        }

        // Also send to coaches (app users with no runner profile but with device IDs)
        val coaches = authenticationRepository.findByRole("coach")
            .filter { it.deviceId != null && it.deviceId!!.isNotEmpty() }

        for (coach in coaches) {
            try {
                firebaseMessageService.sendMessageToDeviceId(coach.deviceId!!, title, body, data)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
