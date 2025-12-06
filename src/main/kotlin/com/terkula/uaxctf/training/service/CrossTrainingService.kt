package com.terkula.uaxctf.training.service

import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.training.model.CrossTraining.CrossTraining
import com.terkula.uaxctf.training.model.CrossTraining.CrossTrainingRecord
import com.terkula.uaxctf.training.model.TrainingComment
import com.terkula.uaxctf.training.model.journal.JournalComment
import com.terkula.uaxctf.training.repository.CrossTrainingRecordRepository
import com.terkula.uaxctf.training.repository.CrossTrainingRepository
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
        val crossTrainingRecordRepository: CrossTrainingRecordRepository,
        val runnerRepository: RunnerRepository,
        val pointsService: com.terkula.uaxctf.statistics.service.PointsService
        ) {

    fun getCrossTrainingActivities(startDate: Date, endDate: Date): CrossTrainingResponse {
        return CrossTrainingResponse(
                crossTrainingRepository.findByDateBetween(startDate, endDate).map {
                    CrossTrainingDTO(it.date, it.distance, it.distanceUnit, it.duration, it.icon, it.uuid,
                            it.name, it.description, it.season, it.team, it.effortLabel, it.crossTrainingType)
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
                    createCrossTrainingRequest.crossTrainingType
            )

            crossTrainingRepository.save(
                    crossTrainingActivity
            )

            return CrossTrainingResponse(listOf(
                    CrossTrainingDTO(crossTrainingActivity.date, crossTrainingActivity.distance, crossTrainingActivity.distanceUnit, crossTrainingActivity.duration, crossTrainingActivity.icon,
                            crossTrainingActivity.uuid, crossTrainingActivity.name, crossTrainingActivity.description,
                            crossTrainingActivity.season, crossTrainingActivity.team, crossTrainingActivity.effortLabel, crossTrainingActivity.crossTrainingType)
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
            )

            crossTrainingRepository.save(
                    crossTraining
            )

            return CrossTrainingResponse(listOf(
                    CrossTrainingDTO(crossTraining.date, crossTraining.distance, crossTraining.distanceUnit, crossTraining.duration, crossTraining.icon,
                            crossTraining.uuid, crossTraining.name, crossTraining.description,
                            crossTraining.season, crossTraining.team, crossTraining.effortLabel, crossTraining.crossTrainingType)
            ))

        } else {

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

            crossTrainingRepository.save(foundCrossTraining)

            return CrossTrainingResponse(listOf(
                    CrossTrainingDTO(foundCrossTraining.date, foundCrossTraining.distance, foundCrossTraining.distanceUnit, foundCrossTraining.duration,
                            foundCrossTraining.icon, foundCrossTraining.uuid, foundCrossTraining.name, foundCrossTraining.description,
                             foundCrossTraining.season,
                            foundCrossTraining.team, foundCrossTraining.effortLabel, foundCrossTraining.crossTrainingType)
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
                            createCrossTrainingRecordRequest.maxPower
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

            crossTrainingRecordRepository.save(existingRecord)

            val comments = trainingCommentRepository.findByTrainingEntityUuid(createCrossTrainingRecordRequest.uuid)
                    .sortedBy { it.timestamp }

            return CrossTrainingRecordResponse(runner, existingRecord, comments)

        }
    }

    fun getRunnersCrossTrainingRecord(runnerId: Int, crossTrainingUuid: String): CrossTrainingRecordResponse? {
        val record = crossTrainingRecordRepository.findByCrossTrainingUuidAndRunnerId(crossTrainingUuid, runnerId).firstOrNull()

        val runner = runnerRepository.findById(runnerId).orElseGet(null)

        return if (record != null) {
            val comments = trainingCommentRepository.findByTrainingEntityUuid(record.uuid).sortedBy { it.timestamp }
            CrossTrainingRecordResponse(runner, record, comments)
        } else {
            null
        }

    }

    fun getCrossTrainingRecord(uuid: String): CrossTrainingRecordResponse? {
        val record = crossTrainingRecordRepository.findByUuid(uuid).firstOrNull()

        return if (record != null) {
            val comments = trainingCommentRepository.findByTrainingEntityUuid(record.uuid).sortedBy { it.timestamp }
            val runner = runnerRepository.findById(record.runnerId).get()
            CrossTrainingRecordResponse(runner, record, comments)
        } else {
            null
        }

    }

    fun getAllCrossTrainingRecordsForActivity(crossTrainingUuid: String): List<CrossTrainingRecordResponse> {
        return crossTrainingRecordRepository.findByCrossTrainingUuid(crossTrainingUuid).map {
            val comments = trainingCommentRepository.findByTrainingEntityUuid(it.uuid).sortedBy { it.timestamp }
            val runner = runnerRepository.findById(it.runnerId).get()
            return@map CrossTrainingRecordResponse(runner, it, comments)
        }
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