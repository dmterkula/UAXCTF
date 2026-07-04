package com.terkula.uaxctf.training.service

import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.statistics.repository.VenueRepository
import com.terkula.uaxctf.statistics.response.ReactionSummary
import com.terkula.uaxctf.training.model.CrossTraining.CrossTraining
import com.terkula.uaxctf.training.model.CrossTraining.CrossTrainingRecord
import com.terkula.uaxctf.training.model.TrainingComment
import com.terkula.uaxctf.training.model.journal.JournalComment
import com.terkula.uaxctf.training.repository.CrossTrainingRecordRepository
import com.terkula.uaxctf.training.repository.CrossTrainingRepository
import com.terkula.uaxctf.training.repository.EventVisibilityRepository
import com.terkula.uaxctf.training.repository.TrainingCommentReactionRepository
import com.terkula.uaxctf.training.repository.TrainingCommentRepository
import com.terkula.uaxctf.training.request.crosstraining.CreateCommentRequest
import com.terkula.uaxctf.training.request.crosstraining.CreateCrossTrainingRecordRequest
import com.terkula.uaxctf.training.request.crosstraining.CreateCrossTrainingRequest
import com.terkula.uaxctf.training.request.journal.CreateJournalCommentRequest
import com.terkula.uaxctf.training.response.crosstraining.CrossTrainingDTO
import com.terkula.uaxctf.training.response.crosstraining.CrossTrainingRecordProfileResponse
import com.terkula.uaxctf.training.response.crosstraining.CrossTrainingRecordResponse
import com.terkula.uaxctf.training.response.crosstraining.CrossTrainingResponse
import org.springframework.stereotype.Service
import java.sql.Date

@Service
class CrossTrainingService(
        val crossTrainingRepository: CrossTrainingRepository,
        val trainingCommentRepository: TrainingCommentRepository,
        val trainingCommentReactionRepository: TrainingCommentReactionRepository,
        val crossTrainingRecordRepository: CrossTrainingRecordRepository,
        val runnerRepository: RunnerRepository,
        val pointsService: com.terkula.uaxctf.statistics.service.PointsService,
        val venueRepository: VenueRepository,
        val eventVisibilityRepository: EventVisibilityRepository
        ) {

    private fun getCommentsWithReactions(entityUuid: String): List<TrainingComment> {
        return trainingCommentRepository.findByTrainingEntityUuid(entityUuid)
            .sortedBy { it.timestamp }
            .map { comment ->
                comment.reactions = trainingCommentReactionRepository.findByCommentUuid(comment.uuid)
                    .groupBy { it.emoji }
                    .map { (emoji, reactions) ->
                        val distinctByUser = reactions.distinctBy { it.reactorUsername }
                        ReactionSummary(
                            emoji = emoji,
                            count = reactions.size,
                            usernames = distinctByUser.map { it.reactorUsername },
                            displayNames = distinctByUser.map { it.reactorDisplayName.ifEmpty { it.reactorUsername } }
                        )
                    }
                    .sortedByDescending { it.count }
                comment
            }
    }

    private fun resolveVenue(venueId: Int?) = venueId?.let { venueRepository.findById(it).orElse(null) }

    fun getCrossTrainingActivities(startDate: Date, endDate: Date): CrossTrainingResponse {
        val activities = crossTrainingRepository.findByDateBetween(startDate, endDate)
        val visibilityMap = eventVisibilityRepository.findByEventUuidIn(activities.map { it.uuid })
            .groupBy { it.eventUuid }
            .mapValues { (_, entries) -> entries.map { it.runnerId } }
        return CrossTrainingResponse(
                activities.map {
                    CrossTrainingDTO(it.date, it.distance, it.distanceUnit, it.duration, it.icon, it.uuid,
                            it.name, it.description, it.season, it.team, it.effortLabel, it.crossTrainingType,
                            resolveVenue(it.venueId), visibilityMap[it.uuid]?.takeIf { ids -> ids.isNotEmpty() })
                }
        )
    }

    fun getCrossTrainingActivitiesForRunnerBetweenDatesForSeason(runnerId: Int, startDate: Date, endDate: Date, season: String): List<CrossTrainingRecordProfileResponse> {
        return crossTrainingRepository.findByDateBetweenAndSeason(startDate, endDate, season)
                .map { it to crossTrainingRecordRepository.findByCrossTrainingUuidAndRunnerId(it.uuid, runnerId) }
                .filter { it.second.isNotEmpty() }
                .map { it.first to it.second.first() }
                .map {
                    CrossTrainingRecordProfileResponse(it.first, it.second)
                }
    }

    fun getCrossTrainingActivitiesForRunnerBetweenDates(runnerId: Int, startDate: Date, endDate: Date): List<CrossTrainingRecordProfileResponse> {
        return crossTrainingRepository.findByDateBetween(startDate, endDate)
                .map { it to crossTrainingRecordRepository.findByCrossTrainingUuidAndRunnerId(it.uuid, runnerId) }
                .filter { it.second.isNotEmpty() }
                .map { it.first to it.second.first() }
                .map {
                    CrossTrainingRecordProfileResponse(it.first, it.second)
                }
    }

    fun createCrossTraining(createCrossTrainingRequest: CreateCrossTrainingRequest): CrossTrainingResponse {

        if (crossTrainingRepository.findByUuid(createCrossTrainingRequest.uuid).isEmpty()) {

            val crossTrainingActivity = CrossTraining(
                    createCrossTrainingRequest.date,
                    createCrossTrainingRequest.distance,
                    createCrossTrainingRequest.distanceUnit,
                    createCrossTrainingRequest.duration,
                    createCrossTrainingRequest.icon,
                    createCrossTrainingRequest.uuid,
                    createCrossTrainingRequest.name,
                    createCrossTrainingRequest.description,
                    createCrossTrainingRequest.season,
                    createCrossTrainingRequest.team,
                    createCrossTrainingRequest.effortLabel,
                    createCrossTrainingRequest.crossTrainingType,
                    createCrossTrainingRequest.venueId
            )

            crossTrainingRepository.save(
                    crossTrainingActivity
            )

            return CrossTrainingResponse(listOf(
                    CrossTrainingDTO(crossTrainingActivity.date, crossTrainingActivity.distance, crossTrainingActivity.distanceUnit, crossTrainingActivity.duration, crossTrainingActivity.icon,
                            crossTrainingActivity.uuid, crossTrainingActivity.name, crossTrainingActivity.description,
                            crossTrainingActivity.season, crossTrainingActivity.team, crossTrainingActivity.effortLabel, crossTrainingActivity.crossTrainingType,
                            resolveVenue(crossTrainingActivity.venueId))
            ))

        } else {
            throw RuntimeException("training run already exists for uuid: " + createCrossTrainingRequest.uuid)
        }

    }

    fun updateCrossTrainingActivity(createCrossTrainingRequest: CreateCrossTrainingRequest): CrossTrainingResponse {

        val foundCrossTraining = crossTrainingRepository.findByUuid(createCrossTrainingRequest.uuid).firstOrNull()

        if (foundCrossTraining == null) {

            val crossTraining = CrossTraining(
                    createCrossTrainingRequest.date,
                    createCrossTrainingRequest.distance,
                    createCrossTrainingRequest.distanceUnit,
                    createCrossTrainingRequest.duration,
                    createCrossTrainingRequest.icon,
                    createCrossTrainingRequest.uuid,
                    createCrossTrainingRequest.name,
                    createCrossTrainingRequest.description,
                    createCrossTrainingRequest.season,
                    createCrossTrainingRequest.team,
                    createCrossTrainingRequest.effortLabel,
                    createCrossTrainingRequest.crossTrainingType,
                    createCrossTrainingRequest.venueId
            )

            crossTrainingRepository.save(
                    crossTraining
            )

            return CrossTrainingResponse(listOf(
                    CrossTrainingDTO(crossTraining.date, crossTraining.distance, crossTraining.distanceUnit, crossTraining.duration, crossTraining.icon,
                            crossTraining.uuid, crossTraining.name, crossTraining.description,
                            crossTraining.season, crossTraining.team, crossTraining.effortLabel, crossTraining.crossTrainingType,
                            resolveVenue(crossTraining.venueId))
            ))

        } else {

            val oldSeason = foundCrossTraining.season
            val seasonChanged = oldSeason != null && oldSeason != createCrossTrainingRequest.season

            foundCrossTraining.date = createCrossTrainingRequest.date
            foundCrossTraining.icon = createCrossTrainingRequest.icon
            foundCrossTraining.duration = createCrossTrainingRequest.duration
            foundCrossTraining.distance = createCrossTrainingRequest.distance
            foundCrossTraining.distanceUnit = createCrossTrainingRequest.distanceUnit
            foundCrossTraining.name = createCrossTrainingRequest.name
            foundCrossTraining.description = createCrossTrainingRequest.description
            foundCrossTraining.season = createCrossTrainingRequest.season
            foundCrossTraining.team = createCrossTrainingRequest.team
            foundCrossTraining.effortLabel = createCrossTrainingRequest.effortLabel
            foundCrossTraining.crossTrainingType = createCrossTrainingRequest.crossTrainingType
            foundCrossTraining.venueId = createCrossTrainingRequest.venueId

            crossTrainingRepository.save(foundCrossTraining)

            if (seasonChanged) {
                try {
                    val recordUuids = crossTrainingRecordRepository
                            .findByCrossTrainingUuid(createCrossTrainingRequest.uuid)
                            .map { it.uuid }
                    pointsService.reattributePointsForSeasonChange(
                            activityUuids = recordUuids,
                            oldSeason = oldSeason!!,
                            newSeason = createCrossTrainingRequest.season,
                            runDate = createCrossTrainingRequest.date
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            return CrossTrainingResponse(listOf(
                    CrossTrainingDTO(foundCrossTraining.date, foundCrossTraining.distance, foundCrossTraining.distanceUnit, foundCrossTraining.duration,
                            foundCrossTraining.icon, foundCrossTraining.uuid, foundCrossTraining.name, foundCrossTraining.description,
                            foundCrossTraining.season, foundCrossTraining.team, foundCrossTraining.effortLabel, foundCrossTraining.crossTrainingType,
                            resolveVenue(foundCrossTraining.venueId))
            ))
        }

    }

    fun deleteCrossTraining(uuid: String): CrossTrainingResponse {

        val foundCrossTraining = crossTrainingRepository.findByUuid(uuid).firstOrNull()

        if (foundCrossTraining == null) {
            return CrossTrainingResponse(emptyList())
        } else {
            val crossTrainingRecords = listOf<CrossTrainingDTO>()

            if (crossTrainingRecords.isEmpty()) {
                crossTrainingRepository.delete(foundCrossTraining)

                return CrossTrainingResponse(listOf(CrossTrainingDTO(
                        foundCrossTraining.date, foundCrossTraining.distance, foundCrossTraining.distanceUnit, foundCrossTraining.duration,
                        foundCrossTraining.icon, foundCrossTraining.uuid, foundCrossTraining.name, foundCrossTraining.description,
                        foundCrossTraining.season,
                        foundCrossTraining.team, foundCrossTraining.effortLabel, foundCrossTraining.crossTrainingType
                )))
            } else {
                // if there are training runs logged for this already, don't delete

                return CrossTrainingResponse(emptyList())
            }

        }
    }

    fun createCrossTrainingRecord(createCrossTrainingRecordRequest: CreateCrossTrainingRecordRequest): CrossTrainingRecordResponse {
        val existingRecord = crossTrainingRecordRepository.findByUuid(createCrossTrainingRecordRequest.uuid).firstOrNull()
        val runner = runnerRepository.findById(createCrossTrainingRecordRequest.runnerId).get()

        if (existingRecord == null) {
           val newRecord = crossTrainingRecordRepository.save(
                    CrossTrainingRecord(
                            createCrossTrainingRecordRequest.uuid,
                            createCrossTrainingRecordRequest.crossTrainingUuid,
                            createCrossTrainingRecordRequest.runnerId,
                            createCrossTrainingRecordRequest.distance,
                            createCrossTrainingRecordRequest.time,
                            createCrossTrainingRecordRequest.notes,
                            createCrossTrainingRecordRequest.coachesNotes,
                            createCrossTrainingRecordRequest.effortLevel,
                            createCrossTrainingRecordRequest.avgHr,
                            createCrossTrainingRecordRequest.maxHr,
                            createCrossTrainingRecordRequest.avgPower,
                            createCrossTrainingRecordRequest.maxPower,
                            createCrossTrainingRecordRequest.crossTrainingType
                    )
            )

            // Award points for NEW cross training only
            try {
                pointsService.earnPoints(com.terkula.uaxctf.statistics.request.EarnPointsRequest(
                        runnerId = createCrossTrainingRecordRequest.runnerId,
                        activityType = "CROSS_TRAINING",
                        activityUuid = createCrossTrainingRecordRequest.uuid,
                        season = createCrossTrainingRecordRequest.season,
                        year = createCrossTrainingRecordRequest.year,
                        description = "Logged cross training activity"
                ))
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return CrossTrainingRecordResponse(runner, newRecord, emptyList())

        } else {
            existingRecord.distance = createCrossTrainingRecordRequest.distance
            existingRecord.time = createCrossTrainingRecordRequest.time
            existingRecord.notes = createCrossTrainingRecordRequest.notes
            existingRecord.coachesNotes = createCrossTrainingRecordRequest.coachesNotes
            existingRecord.effortLevel = createCrossTrainingRecordRequest.effortLevel
            existingRecord.avgHr = createCrossTrainingRecordRequest.avgHr
            existingRecord.maxHr = createCrossTrainingRecordRequest.maxHr
            existingRecord.avgPower = createCrossTrainingRecordRequest.avgPower
            existingRecord.maxPower = createCrossTrainingRecordRequest.maxPower
            existingRecord.crossTrainingType = createCrossTrainingRecordRequest.crossTrainingType

            crossTrainingRecordRepository.save(existingRecord)

            val comments = getCommentsWithReactions(createCrossTrainingRecordRequest.uuid)

            return CrossTrainingRecordResponse(runner, existingRecord, comments)

        }
    }

    fun getRunnersCrossTrainingRecord(runnerId: Int, crossTrainingUuid: String): CrossTrainingRecordResponse? {
        val record = crossTrainingRecordRepository.findByCrossTrainingUuidAndRunnerId(crossTrainingUuid, runnerId).firstOrNull()

        val runner = runnerRepository.findById(runnerId).orElseGet(null)

        return if (record != null) {
            val comments = getCommentsWithReactions(record.uuid)
            CrossTrainingRecordResponse(runner, record, comments)
        } else {
            null
        }

    }

    fun getCrossTrainingRecord(uuid: String): CrossTrainingRecordResponse? {
        val record = crossTrainingRecordRepository.findByUuid(uuid).firstOrNull()

        return if (record != null) {
            val comments = getCommentsWithReactions(record.uuid)
            val runner = runnerRepository.findById(record.runnerId).get()
            CrossTrainingRecordResponse(runner, record, comments)
        } else {
            null
        }

    }

    fun getAllCrossTrainingRecordsForActivity(crossTrainingUuid: String): List<CrossTrainingRecordResponse> {
        return crossTrainingRecordRepository.findByCrossTrainingUuid(crossTrainingUuid).map {
            val comments = getCommentsWithReactions(it.uuid)
            val runner = runnerRepository.findById(it.runnerId).get()
            return@map CrossTrainingRecordResponse(runner, it, comments)
        }
    }

    fun forceReattributePoints(crossTrainingUuid: String, fromSeason: String, toSeason: String): String {
        val crossTraining = crossTrainingRepository.findByUuid(crossTrainingUuid).firstOrNull()
                ?: return "Cross training activity not found: $crossTrainingUuid"

        val recordUuids = crossTrainingRecordRepository
                .findByCrossTrainingUuid(crossTrainingUuid)
                .map { it.uuid }

        if (recordUuids.isEmpty()) {
            return "No runner records found for cross training activity: $crossTrainingUuid"
        }

        pointsService.reattributePointsForSeasonChange(
                activityUuids = recordUuids,
                oldSeason = fromSeason,
                newSeason = toSeason,
                runDate = crossTraining.date
        )

        return "Re-attributed points for ${recordUuids.size} runner(s): $fromSeason → $toSeason"
    }

    fun createComment(createCommentRequest: CreateCommentRequest): TrainingComment {
        return trainingCommentRepository.save(
                TrainingComment(
                        createCommentRequest.uuid,
                        createCommentRequest.referenceUuid,
                        createCommentRequest.madeBy,
                        createCommentRequest.message,
                        createCommentRequest.timestamp
                )
        )
    }

}