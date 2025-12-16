package com.terkula.uaxctf.training.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.statistics.request.EarnPointsRequest
import com.terkula.uaxctf.statistics.service.PointsService
import com.terkula.uaxctf.training.model.TrainingComment
import com.terkula.uaxctf.training.model.lifting.*
import com.terkula.uaxctf.training.repository.*
import com.terkula.uaxctf.training.request.lifting.*
import com.terkula.uaxctf.training.response.lifting.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.Date
import java.sql.Timestamp
import java.util.*

@Service
class LiftingService(
    private val exerciseRepository: ExerciseRepository,
    private val liftingActivityRepository: LiftingActivityRepository,
    private val liftingRecordRepository: LiftingRecordRepository,
    private val liftingPRRepository: LiftingPRRepository,
    private val liftingPRHistoryRepository: LiftingPRHistoryRepository,
    private val trainingCommentRepository: TrainingCommentRepository,
    private val runnerRepository: RunnerRepository,
    private val pointsService: PointsService,
    private val objectMapper: ObjectMapper
) {

    // ===== Exercise Management =====

    fun getExercises(team: String): ExercisesResponse {
        val exercises = exerciseRepository.findByIsGlobalTrueOrTeam(team)
        return ExercisesResponse(exercises.map { it.toDTO() })
    }

    @Transactional
    fun createExercise(request: CreateExerciseRequest): ExerciseDTO {
        val exercise = Exercise(
            uuid = request.uuid,
            name = request.name,
            description = request.description,
            category = request.category,
            exerciseType = request.exerciseType,
            isGlobal = request.isGlobal,
            team = request.team,
            createdBy = request.createdBy,
            defaultWeightUnit = request.defaultWeightUnit
        )

        val saved = exerciseRepository.save(exercise)
        return saved.toDTO()
    }

    @Transactional
    fun updateExercise(request: UpdateExerciseRequest): ExerciseDTO {
        val exercise = exerciseRepository.findByUuid(request.uuid)
            ?: throw RuntimeException("Exercise not found: ${request.uuid}")

        exercise.name = request.name
        exercise.description = request.description
        exercise.category = request.category
        exercise.exerciseType = request.exerciseType
        exercise.defaultWeightUnit = request.defaultWeightUnit

        val saved = exerciseRepository.save(exercise)
        return saved.toDTO()
    }

    @Transactional
    fun deleteExercise(uuid: String) {
        val exercise = exerciseRepository.findByUuid(uuid)
            ?: throw RuntimeException("Exercise not found: $uuid")

        // Check if exercise is used in any PRs
        val prs = liftingPRRepository.findByExerciseUuid(uuid)
        if (prs.isNotEmpty()) {
            throw RuntimeException("Cannot delete exercise that has PRs associated with it")
        }

        exerciseRepository.delete(exercise)
    }

    // ===== Activity Management =====

    fun getLiftingActivities(startDate: Date, endDate: Date, team: String?): LiftingActivitiesResponse {
        val activities = if (team != null) {
            liftingActivityRepository.findByDateBetweenAndTeam(startDate, endDate, team)
        } else {
            liftingActivityRepository.findByDateBetween(startDate, endDate)
        }

        return LiftingActivitiesResponse(activities.map { it.toDTO() })
    }

    @Transactional
    fun createLiftingActivity(request: CreateLiftingActivityRequest): LiftingActivityDTO {
        val activity = LiftingActivity(
            uuid = request.uuid,
            name = request.name,
            description = request.description,
            date = request.date,
            duration = request.duration,
            icon = request.icon,
            season = request.season,
            team = request.team,
            suggestedExercises = request.suggestedExercises?.let { objectMapper.writeValueAsString(it) }
        )

        val saved = liftingActivityRepository.save(activity)
        return saved.toDTO()
    }

    @Transactional
    fun updateLiftingActivity(request: UpdateLiftingActivityRequest): LiftingActivityDTO {
        val activity = liftingActivityRepository.findByUuid(request.uuid)
            ?: throw RuntimeException("Lifting activity not found: ${request.uuid}")

        activity.name = request.name
        activity.description = request.description
        activity.date = request.date
        activity.duration = request.duration
        activity.icon = request.icon
        activity.season = request.season
        activity.team = request.team
        activity.suggestedExercises = request.suggestedExercises?.let { objectMapper.writeValueAsString(it) }

        val saved = liftingActivityRepository.save(activity)
        return saved.toDTO()
    }

    @Transactional
    fun deleteLiftingActivity(uuid: String) {
        val activity = liftingActivityRepository.findByUuid(uuid)
            ?: throw RuntimeException("Lifting activity not found: $uuid")

        // Check if any records exist for this activity
        val records = liftingRecordRepository.findByLiftingActivityUuid(uuid)
        if (records.isNotEmpty()) {
            throw RuntimeException("Cannot delete activity that has records logged")
        }

        liftingActivityRepository.delete(activity)
    }

    // ===== Record Management =====

    fun getLiftingRecord(uuid: String): LiftingRecordResponse? {
        val record = liftingRecordRepository.findByUuid(uuid) ?: return null
        val runner = runnerRepository.findById(record.runnerId).orElse(null) ?: return null
        val comments = trainingCommentRepository.findByTrainingEntityUuid(uuid).sortedBy { it.timestamp }

        val recordDTO = parseRecordToDTO(record)
        val prs = findHistoricalPRsForRecord(record, recordDTO.exercises)

        return LiftingRecordResponse(runner, recordDTO, comments, prs)
    }

    fun getLiftingRecordsForActivity(activityUuid: String): List<LiftingRecordResponse> {
        val records = liftingRecordRepository.findByLiftingActivityUuid(activityUuid)
        return records.map { record ->
            val runner = runnerRepository.findById(record.runnerId).get()
            val comments = trainingCommentRepository.findByTrainingEntityUuid(record.uuid)
                .sortedBy { it.timestamp }
            val recordDTO = parseRecordToDTO(record)
            val prs = findHistoricalPRsForRecord(record, recordDTO.exercises)
            LiftingRecordResponse(runner, recordDTO, comments, prs)
        }
    }

    fun getRunnerLiftingRecord(runnerId: Int, activityUuid: String): LiftingRecordResponse? {
        val record = liftingRecordRepository.findByLiftingActivityUuidAndRunnerId(activityUuid, runnerId)
            ?: return null
        val runner = runnerRepository.findById(runnerId).orElse(null) ?: return null
        val comments = trainingCommentRepository.findByTrainingEntityUuid(record.uuid)
            .sortedBy { it.timestamp }

        val recordDTO = parseRecordToDTO(record)
        val prs = findHistoricalPRsForRecord(record, recordDTO.exercises)

        return LiftingRecordResponse(runner, recordDTO, comments, prs)
    }

    @Transactional
    fun createLiftingRecord(request: CreateLiftingRecordRequest): LiftingRecordResponse {
        val existingRecord = liftingRecordRepository.findByLiftingActivityUuidAndRunnerId(
            request.liftingActivityUuid,
            request.runnerId
        )

        if (existingRecord != null) {
            // Update existing record instead
            return updateLiftingRecord(UpdateLiftingRecordRequest(
                uuid = existingRecord.uuid,
                exercises = request.exercises,
                totalDuration = request.totalDuration,
                notes = request.notes,
                coachesNotes = request.coachesNotes,
                effortLevel = request.effortLevel
            ))
        }

        // Serialize exercises to JSON
        val exercisesJson = objectMapper.writeValueAsString(request.exercises)

        val record = LiftingRecord(
            uuid = request.uuid,
            liftingActivityUuid = request.liftingActivityUuid,
            runnerId = request.runnerId,
            exercisesData = exercisesJson,
            totalDuration = request.totalDuration,
            notes = request.notes,
            coachesNotes = request.coachesNotes,
            effortLevel = request.effortLevel
        )

        val saved = liftingRecordRepository.save(record)

        // Award points for new lifting record
        try {
            pointsService.earnPoints(EarnPointsRequest(
                runnerId = request.runnerId,
                activityType = "LIFTING",
                activityUuid = request.uuid,
                season = request.season,
                year = request.year,
                description = "Logged lifting session"
            ))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Calculate and save PRs
        val recordDTO = parseRecordToDTO(saved)
        val prsAchieved = calculateAndSavePRs(saved, recordDTO.exercises)

        val runner = runnerRepository.findById(request.runnerId).get()
        val comments = emptyList<TrainingComment>()

        return LiftingRecordResponse(runner, recordDTO, comments, prsAchieved)
    }

    @Transactional
    fun updateLiftingRecord(request: UpdateLiftingRecordRequest): LiftingRecordResponse {
        val record = liftingRecordRepository.findByUuid(request.uuid)
            ?: throw RuntimeException("Lifting record not found: ${request.uuid}")

        // Update record
        val exercisesJson = objectMapper.writeValueAsString(request.exercises)
        record.exercisesData = exercisesJson
        record.totalDuration = request.totalDuration
        record.notes = request.notes
        record.coachesNotes = request.coachesNotes
        record.effortLevel = request.effortLevel

        val saved = liftingRecordRepository.save(record)

        // Recalculate PRs
        val recordDTO = parseRecordToDTO(saved)
        val prsAchieved = calculateAndSavePRs(saved, recordDTO.exercises)

        val runner = runnerRepository.findById(record.runnerId).get()
        val comments = trainingCommentRepository.findByTrainingEntityUuid(record.uuid)
            .sortedBy { it.timestamp }

        return LiftingRecordResponse(runner, recordDTO, comments, prsAchieved)
    }

    // ===== PR Management =====

    fun getRunnerPRs(
        runnerId: Int,
        exerciseUuid: String?,
        startDate: Timestamp? = null,
        endDate: Timestamp? = null
    ): LiftingPRsResponse {
        // If dates provided, compute PRs from records in that time frame
        if (startDate != null && endDate != null) {
            return computePRsFromTimeFrame(runnerId, startDate, endDate, exerciseUuid)
        }

        // Otherwise, return stored all-time PRs
        val prs = if (exerciseUuid != null) {
            liftingPRRepository.findByRunnerIdAndExerciseUuid(runnerId, exerciseUuid)
        } else {
            liftingPRRepository.findByRunnerId(runnerId)
        }

        return LiftingPRsResponse(prs.map { it.toDTO() })
    }

    private fun computePRsFromTimeFrame(
        runnerId: Int,
        startDate: Timestamp,
        endDate: Timestamp,
        exerciseUuid: String?
    ): LiftingPRsResponse {

        // 1. Get all records in date range (by activity date, not logged date)
        val startDateSql = Date(startDate.time)
        val endDateSql = Date(endDate.time)

        val records = liftingRecordRepository.findByRunnerIdAndActivityDateBetween(
            runnerId, startDateSql, endDateSql
        )

        if (records.isEmpty()) {
            return LiftingPRsResponse(emptyList())
        }

        // 2. Fetch all activities for these records (batch load)
        val activityUuids = records.map { it.liftingActivityUuid }.distinct()
        val activities = activityUuids.mapNotNull { uuid ->
            liftingActivityRepository.findByUuid(uuid)
        }.associateBy { it.uuid }

        // 3. Track best performance per exercise
        data class BestPerformance(
            val exercise: Exercise,
            val prType: String,
            val weight: Double? = null,
            val repNumber: Int? = null,
            val duration: String? = null,
            val recordUuid: String,
            val activityUuid: String,
            val achievedDate: Timestamp
        )

        val bestByExercise = mutableMapOf<Pair<String, String>, BestPerformance>()

        // 4. Process each record
        records.forEach { record ->
            val recordDTO = parseRecordToDTO(record)

            recordDTO.exercises
                .filter { exerciseUuid == null || it.exercise.uuid == exerciseUuid }
                .forEach { exerciseEntry ->

                    val exercise = exerciseRepository.findByUuid(exerciseEntry.exercise.uuid)
                        ?: return@forEach

                    when (exercise.exerciseType) {
                        "WEIGHT", "BODYWEIGHT" -> {
                            val allReps = exerciseEntry.sets.flatMap { it.reps }
                            if (allReps.isEmpty()) return@forEach

                            val maxWeight = allReps.maxByOrNull { it.weight }?.weight
                                ?: return@forEach

                            // Find the max number of reps in any single set at the max weight
                            val repsAtMaxWeight = exerciseEntry.sets
                                .map { set -> set.reps.count { it.weight == maxWeight } }
                                .filter { it > 0 }
                                .maxOrNull() ?: 1

                            val key = Pair(exercise.uuid, "MAX_WEIGHT")
                            val existing = bestByExercise[key]

                            if (existing == null || maxWeight > (existing.weight ?: 0.0)) {
                                bestByExercise[key] = BestPerformance(
                                    exercise = exercise,
                                    prType = "MAX_WEIGHT",
                                    weight = maxWeight,
                                    repNumber = repsAtMaxWeight,
                                    recordUuid = record.uuid,
                                    activityUuid = record.liftingActivityUuid,
                                    achievedDate = record.dateLogged
                                )
                            }
                        }

                        "DURATION" -> {
                            val durations = exerciseEntry.sets.mapNotNull { it.duration }
                            if (durations.isEmpty()) return@forEach

                            val maxDuration = durations.maxByOrNull { parseDuration(it) }
                                ?: return@forEach

                            // Get the set with the max duration to extract weight info
                            val maxDurationSet = exerciseEntry.sets.firstOrNull { it.duration == maxDuration }

                            // If the set has reps with weights, get the max weight used during that duration
                            val maxWeight = maxDurationSet?.reps?.maxByOrNull { it.weight }?.weight
                            val repsAtMaxWeight = if (maxWeight != null && maxDurationSet != null) {
                                maxDurationSet.reps.count { it.weight == maxWeight }
                            } else null

                            val key = Pair(exercise.uuid, "MAX_DURATION")
                            val existing = bestByExercise[key]
                            val newDurationSeconds = parseDuration(maxDuration)
                            val existingDurationSeconds = existing?.duration?.let { parseDuration(it) } ?: 0L

                            if (existing == null || newDurationSeconds > existingDurationSeconds) {
                                bestByExercise[key] = BestPerformance(
                                    exercise = exercise,
                                    prType = "MAX_DURATION",
                                    weight = maxWeight,
                                    repNumber = repsAtMaxWeight,
                                    duration = maxDuration,
                                    recordUuid = record.uuid,
                                    activityUuid = record.liftingActivityUuid,
                                    achievedDate = record.dateLogged
                                )
                            }
                        }
                    }
                }
        }

        // 5. Convert to DTOs with activity info
        val prDTOs = bestByExercise.values.map { best ->
            val activity = activities[best.activityUuid]

            LiftingPRDTO(
                uuid = UUID.randomUUID().toString(), // Generated, not from DB
                runnerId = runnerId,
                exercise = best.exercise.toDTO(),
                prType = best.prType,
                weight = best.weight,
                weightUnit = if (best.weight != null) best.exercise.defaultWeightUnit else null,
                repNumber = best.repNumber,
                duration = best.duration,
                achievedDate = best.achievedDate,
                liftingRecordUuid = best.recordUuid,
                activityName = activity?.name,
                activityDate = activity?.date?.toString()
            )
        }

        return LiftingPRsResponse(prDTOs)
    }

    // ===== Runner Activity Management =====

    fun getRunnerLiftingActivities(
        runnerId: Int,
        startDate: Timestamp,
        endDate: Timestamp
    ): RunnerLiftingActivitiesResponse {
        // Validate runner exists
        runnerRepository.findById(runnerId)
            .orElseThrow { RuntimeException("Runner not found: $runnerId") }

        // Convert Timestamp to Date for queries
        val startDateSql = Date(startDate.time)
        val endDateSql = Date(endDate.time)

        // Get all records for this runner in the date range (by activity date, not logged date)
        val records = liftingRecordRepository.findByRunnerIdAndActivityDateBetween(
            runnerId, startDateSql, endDateSql
        )

        // Get the activity UUIDs from the records
        val recordActivityUuids = records.map { it.liftingActivityUuid }.toSet()

        // Get all activities in the date range
        val allActivities = liftingActivityRepository.findByDateBetween(startDateSql, endDateSql)

        // Map records by activity UUID for quick lookup
        val recordsByActivityUuid = records.associateBy { it.liftingActivityUuid }

        // Build response DTOs
        val activityDTOs = allActivities.map { activity ->
            val record = recordsByActivityUuid[activity.uuid]
            val recordDTO = record?.let { parseRecordToDTO(it) }

            RunnerLiftingActivityDTO(
                activity = activity.toDTO(),
                record = recordDTO,
                recordExists = record != null
            )
        }.sortedByDescending { it.activity.date }

        val completedCount = activityDTOs.count { it.recordExists }

        return RunnerLiftingActivitiesResponse(
            runnerId = runnerId,
            startDate = startDate,
            endDate = endDate,
            activities = activityDTOs,
            totalActivities = activityDTOs.size,
            completedActivities = completedCount
        )
    }

    // ===== Exercise History =====

    fun getExerciseHistory(
        runnerId: Int,
        exerciseUuid: String,
        startDate: Timestamp,
        endDate: Timestamp
    ): ExerciseHistoryResponse {

        // Step 1: Validate exercise exists
        val exercise = exerciseRepository.findByUuid(exerciseUuid)
            ?: throw RuntimeException("Exercise not found: $exerciseUuid")

        // Step 2: Validate runner exists
        runnerRepository.findById(runnerId)
            .orElseThrow { RuntimeException("Runner not found: $runnerId") }

        // Step 3: Fetch all lifting records in date range for this runner
        // Convert Timestamps to Dates for the query (activity dates are stored as dates, not timestamps)
        val startDateSql = Date(startDate.time)
        val endDateSql = Date(endDate.time)

        val allRecords = liftingRecordRepository.findByRunnerIdAndActivityDateBetween(
            runnerId, startDateSql, endDateSql
        )

        println("DEBUG getExerciseHistory: runnerId=$runnerId, exerciseUuid=$exerciseUuid")
        println("DEBUG getExerciseHistory: startDate=$startDate, endDate=$endDate")
        println("DEBUG getExerciseHistory: Found ${allRecords.size} records for runner in date range (by activity date)")

        if (allRecords.isEmpty()) {
            println("DEBUG getExerciseHistory: No records found - returning empty response")
            return ExerciseHistoryResponse(
                runnerId = runnerId,
                exercise = exercise.toDTO(),
                startDate = startDate,
                endDate = endDate,
                sessions = emptyList(),
                totalSessions = 0,
                currentPR = null
            )
        }

        // Step 4: Filter records to only those containing the specified exercise
        val recordsWithExercise = mutableListOf<Pair<LiftingRecord, LiftingExerciseEntryDTO>>()

        allRecords.forEach { record ->
            val recordDTO = parseRecordToDTO(record)
            val exerciseEntry = recordDTO.exercises.find { it.exercise.uuid == exerciseUuid }

            if (exerciseEntry != null) {
                recordsWithExercise.add(Pair(record, exerciseEntry))
            }
        }

        println("DEBUG getExerciseHistory: Found ${recordsWithExercise.size} records containing exercise $exerciseUuid")

        if (recordsWithExercise.isEmpty()) {
            println("DEBUG getExerciseHistory: No records contain the specified exercise - returning empty response")
            return ExerciseHistoryResponse(
                runnerId = runnerId,
                exercise = exercise.toDTO(),
                startDate = startDate,
                endDate = endDate,
                sessions = emptyList(),
                totalSessions = 0,
                currentPR = null
            )
        }

        // Step 5: Batch load all activities for these records
        val activityUuids = recordsWithExercise.map { it.first.liftingActivityUuid }.distinct()
        val activities = activityUuids.mapNotNull { uuid ->
            liftingActivityRepository.findByUuid(uuid)
        }.associateBy { it.uuid }

        // Step 6: Get all historical PRs for this runner+exercise to determine which sessions were PRs
        val allPRHistory = liftingPRHistoryRepository.findByRunnerIdAndExerciseUuid(runnerId, exerciseUuid)
        val prRecordUuids = allPRHistory.map { it.liftingRecordUuid }.toSet()

        // Group PR history by record UUID to get the PR details for each session
        val prByRecordUuid = allPRHistory.groupBy { it.liftingRecordUuid }
            .mapValues { (_, prs) -> prs.firstOrNull() }

        // Step 7: Transform to session DTOs
        val sessions = recordsWithExercise.map { (record, exerciseEntry) ->
            val activity = activities[record.liftingActivityUuid]
            val prHistory = prByRecordUuid[record.uuid]
            val isPR = prRecordUuids.contains(record.uuid)

            val prValue = if (isPR && prHistory != null) {
                when (prHistory.prType) {
                    "MAX_WEIGHT" -> "${prHistory.weight} ${prHistory.weightUnit}"
                    "MAX_DURATION" -> prHistory.duration
                    else -> null
                }
            } else null

            ExerciseHistorySessionDTO(
                sessionUuid = record.uuid,
                activityName = activity?.name ?: "Unknown Activity",
                activityDate = activity?.date?.toString() ?: "",
                dateLogged = record.dateLogged,
                exerciseEntry = exerciseEntry,
                sessionNotes = record.notes,
                exerciseNotes = exerciseEntry.notes,
                effortLevel = record.effortLevel,
                isPR = isPR,
                prType = prHistory?.prType,
                prValue = prValue
            )
        }

        // Step 8: Sort by date descending (most recent first)
        val sortedSessions = sessions.sortedByDescending { it.dateLogged }

        // Step 9: Get current all-time PR for context
        val currentPR = liftingPRRepository.findByRunnerIdAndExerciseUuid(runnerId, exerciseUuid)
            .firstOrNull()
            ?.toDTO()

        return ExerciseHistoryResponse(
            runnerId = runnerId,
            exercise = exercise.toDTO(),
            startDate = startDate,
            endDate = endDate,
            sessions = sortedSessions,
            totalSessions = sortedSessions.size,
            currentPR = currentPR
        )
    }

    /**
     * Calculate PRs from a lifting record and save new PRs if they exceed existing ones
     */
    private fun calculateAndSavePRs(
        record: LiftingRecord,
        exercises: List<LiftingExerciseEntryDTO>
    ): List<LiftingPRDTO> {
        val prsAchieved = mutableListOf<LiftingPRDTO>()

        // Get the activity date to use as the PR achieved date
        val activity = liftingActivityRepository.findByUuid(record.liftingActivityUuid)
            ?: throw RuntimeException("Lifting activity not found: ${record.liftingActivityUuid}")
        val activityDate = Timestamp(activity.date.time)

        exercises.forEach { exerciseEntry ->
            val exercise = exerciseRepository.findByUuid(exerciseEntry.exercise.uuid) ?: return@forEach

            when (exercise.exerciseType) {
                "WEIGHT", "BODYWEIGHT" -> {
                    // Find max single rep weight across all sets
                    val allReps = exerciseEntry.sets
                        .flatMap { it.reps }

                    if (allReps.isEmpty()) return@forEach

                    val maxWeight = allReps.maxByOrNull { it.weight }?.weight ?: return@forEach

                    // Find the max number of reps in any single set at the max weight
                    val repsAtMaxWeight = exerciseEntry.sets
                        .map { set -> set.reps.count { it.weight == maxWeight } }
                        .filter { it > 0 }
                        .maxOrNull() ?: 1

                    val existingPR = liftingPRRepository.findByRunnerIdAndExerciseUuidAndPrType(
                        record.runnerId, exercise.uuid, "MAX_WEIGHT"
                    )

                    if (existingPR == null || maxWeight > (existingPR.weight ?: 0.0)) {
                        val prToSave = existingPR ?: LiftingPR(
                            uuid = UUID.randomUUID().toString(),
                            runnerId = record.runnerId,
                            exerciseUuid = exercise.uuid,
                            prType = "MAX_WEIGHT",
                            weight = maxWeight,
                            weightUnit = exercise.defaultWeightUnit,
                            repNumber = repsAtMaxWeight,
                            duration = null,
                            achievedDate = activityDate,
                            liftingRecordUuid = record.uuid
                        )

                        // Update existing PR fields
                        if (existingPR != null) {
                            prToSave.weight = maxWeight
                            prToSave.weightUnit = exercise.defaultWeightUnit
                            prToSave.repNumber = repsAtMaxWeight
                            prToSave.achievedDate = activityDate
                            prToSave.liftingRecordUuid = record.uuid
                        }

                        val saved = liftingPRRepository.save(prToSave)

                        // Log to PR history
                        liftingPRHistoryRepository.save(LiftingPRHistory(
                            uuid = UUID.randomUUID().toString(),
                            runnerId = record.runnerId,
                            exerciseUuid = exercise.uuid,
                            prType = "MAX_WEIGHT",
                            weight = maxWeight,
                            weightUnit = exercise.defaultWeightUnit,
                            repNumber = repsAtMaxWeight,
                            duration = null,
                            achievedDate = activityDate,
                            liftingRecordUuid = record.uuid
                        ))

                        prsAchieved.add(saved.toDTO())
                    }
                }

                "DURATION" -> {
                    // Find longest duration across all sets
                    val durations = exerciseEntry.sets.mapNotNull { it.duration }
                    if (durations.isEmpty()) return@forEach

                    val maxDuration = durations.maxByOrNull { parseDuration(it) } ?: return@forEach

                    // Get the set with the max duration to extract weight info
                    val maxDurationSet = exerciseEntry.sets.firstOrNull { it.duration == maxDuration }

                    // If the set has reps with weights, get the max weight used during that duration
                    val maxWeight = maxDurationSet?.reps?.maxByOrNull { it.weight }?.weight
                    val repsAtMaxWeight = if (maxWeight != null && maxDurationSet != null) {
                        maxDurationSet.reps.count { it.weight == maxWeight }
                    } else null

                    val existingPR = liftingPRRepository.findByRunnerIdAndExerciseUuidAndPrType(
                        record.runnerId, exercise.uuid, "MAX_DURATION"
                    )

                    val existingDurationSeconds = existingPR?.duration?.let { parseDuration(it) } ?: 0L
                    val newDurationSeconds = parseDuration(maxDuration)

                    if (existingPR == null || newDurationSeconds > existingDurationSeconds) {
                        val prToSave = existingPR ?: LiftingPR(
                            uuid = UUID.randomUUID().toString(),
                            runnerId = record.runnerId,
                            exerciseUuid = exercise.uuid,
                            prType = "MAX_DURATION",
                            weight = maxWeight,
                            weightUnit = if (maxWeight != null) exercise.defaultWeightUnit else null,
                            repNumber = repsAtMaxWeight,
                            duration = maxDuration,
                            achievedDate = activityDate,
                            liftingRecordUuid = record.uuid
                        )

                        // Update existing PR fields
                        if (existingPR != null) {
                            prToSave.weight = maxWeight
                            prToSave.weightUnit = if (maxWeight != null) exercise.defaultWeightUnit else null
                            prToSave.repNumber = repsAtMaxWeight
                            prToSave.duration = maxDuration
                            prToSave.achievedDate = activityDate
                            prToSave.liftingRecordUuid = record.uuid
                        }

                        val saved = liftingPRRepository.save(prToSave)

                        // Log to PR history
                        liftingPRHistoryRepository.save(LiftingPRHistory(
                            uuid = UUID.randomUUID().toString(),
                            runnerId = record.runnerId,
                            exerciseUuid = exercise.uuid,
                            prType = "MAX_DURATION",
                            weight = maxWeight,
                            weightUnit = if (maxWeight != null) exercise.defaultWeightUnit else null,
                            repNumber = repsAtMaxWeight,
                            duration = maxDuration,
                            achievedDate = activityDate,
                            liftingRecordUuid = record.uuid
                        ))

                        prsAchieved.add(saved.toDTO())
                    }
                }
            }
        }

        return prsAchieved
    }

    /**
     * Parse duration string (MM:SS or SS) to seconds
     */
    private fun parseDuration(duration: String): Long {
        val parts = duration.split(":")
        return if (parts.size == 2) {
            // MM:SS format
            (parts[0].toLongOrNull() ?: 0) * 60 + (parts[1].toLongOrNull() ?: 0)
        } else if (parts.size == 3) {
            // HH:MM:SS format
            (parts[0].toLongOrNull() ?: 0) * 3600 +
                    (parts[1].toLongOrNull() ?: 0) * 60 +
                    (parts[2].toLongOrNull() ?: 0)
        } else {
            // Just seconds
            duration.toLongOrNull() ?: 0
        }
    }

    /**
     * Find existing PRs for exercises in a record (for display)
     * Uses historical PRs based on the activity date of the record
     */
    private fun findPRsForRecord(runnerId: Int, exercises: List<LiftingExerciseEntryDTO>): List<LiftingPRDTO> {
        val exerciseUuids = exercises.map { it.exercise.uuid }
        val allRunnerPRs = liftingPRRepository.findByRunnerId(runnerId)

        return allRunnerPRs.filter { pr ->
            exerciseUuids.contains(pr.exerciseUuid)
        }.map { it.toDTO() }
    }

    /**
     * Find PRs that were valid at the time of the record's activity date
     * This is used to show what the PR was when the record was logged, not the current PR
     */
    private fun findHistoricalPRsForRecord(
        record: LiftingRecord,
        exercises: List<LiftingExerciseEntryDTO>
    ): List<LiftingPRDTO> {
        // Get the activity to get the activity date
        val activity = liftingActivityRepository.findByUuid(record.liftingActivityUuid)
            ?: return emptyList()

        // Convert activity date to Timestamp for comparison
        val activityTimestamp = Timestamp(activity.date.time)

        val historicalPRs = mutableListOf<LiftingPRDTO>()

        exercises.forEach { exerciseEntry ->
            val exercise = exerciseRepository.findByUuid(exerciseEntry.exercise.uuid) ?: return@forEach

            // For each PR type, find the most recent PR as of the activity date
            val prTypes = when (exercise.exerciseType) {
                "WEIGHT", "BODYWEIGHT" -> listOf("MAX_WEIGHT")
                "DURATION" -> listOf("MAX_DURATION")
                else -> emptyList()
            }

            prTypes.forEach { prType ->
                val historicalPR = liftingPRHistoryRepository
                    .findHistoricalPRs(
                        record.runnerId,
                        exercise.uuid,
                        prType,
                        activityTimestamp
                    )
                    .firstOrNull()

                if (historicalPR != null) {
                    historicalPRs.add(LiftingPRDTO(
                        uuid = historicalPR.uuid,
                        runnerId = historicalPR.runnerId,
                        exercise = exercise.toDTO(),
                        prType = historicalPR.prType,
                        weight = historicalPR.weight,
                        weightUnit = historicalPR.weightUnit,
                        repNumber = historicalPR.repNumber,
                        duration = historicalPR.duration,
                        achievedDate = historicalPR.achievedDate,
                        liftingRecordUuid = historicalPR.liftingRecordUuid
                    ))
                }
            }
        }

        return historicalPRs
    }

    // ===== Comment Management =====

    @Transactional
    fun createComment(request: CreateLiftingCommentRequest): TrainingComment {
        return trainingCommentRepository.save(
            TrainingComment(
                uuid = request.uuid,
                trainingEntityUuid = request.liftingRecordUuid,
                madeBy = request.madeBy,
                message = request.message,
                timestamp = request.timestamp
            )
        )
    }

    // ===== Helper Methods =====

    /**
     * Parse LiftingRecord entity to LiftingRecordDTO with full nested structure
     */
    private fun parseRecordToDTO(record: LiftingRecord): LiftingRecordDTO {
        val exerciseEntries: List<LiftingExerciseEntryRequest> =
            objectMapper.readValue(record.exercisesData)

        val exerciseDTOs = exerciseEntries.map { entry ->
            val exercise = exerciseRepository.findByUuid(entry.exerciseUuid)
                ?: throw RuntimeException("Exercise not found: ${entry.exerciseUuid}")

            LiftingExerciseEntryDTO(
                uuid = entry.uuid,
                exercise = exercise.toDTO(),
                sets = entry.sets.map { set ->
                    LiftingSetDTO(
                        uuid = set.uuid,
                        setNumber = set.setNumber,
                        reps = set.reps.map { rep ->
                            LiftingRepDTO(
                                uuid = rep.uuid,
                                repNumber = rep.repNumber,
                                weight = rep.weight
                            )
                        },
                        duration = set.duration,
                        restTime = set.restTime,
                        notes = set.notes
                    )
                },
                orderIndex = entry.orderIndex,
                notes = entry.notes
            )
        }

        return LiftingRecordDTO(
            uuid = record.uuid,
            liftingActivityUuid = record.liftingActivityUuid,
            runnerId = record.runnerId,
            exercises = exerciseDTOs,
            totalDuration = record.totalDuration,
            notes = record.notes,
            coachesNotes = record.coachesNotes,
            effortLevel = record.effortLevel,
            dateLogged = record.dateLogged
        )
    }

    // ===== Extension Functions for Entity to DTO Mapping =====

    private fun Exercise.toDTO() = ExerciseDTO(
        uuid = this.uuid,
        name = this.name,
        description = this.description,
        category = this.category,
        exerciseType = this.exerciseType,
        isGlobal = this.isGlobal,
        team = this.team,
        createdBy = this.createdBy,
        defaultWeightUnit = this.defaultWeightUnit
    )

    private fun LiftingActivity.toDTO() = LiftingActivityDTO(
        uuid = this.uuid,
        name = this.name,
        description = this.description,
        date = this.date.toString(),
        duration = this.duration,
        icon = this.icon,
        season = this.season,
        team = this.team,
        suggestedExercises = this.suggestedExercises?.let {
            objectMapper.readValue<List<String>>(it)
        }
    )

    private fun LiftingPR.toDTO(): LiftingPRDTO {
        val exercise = exerciseRepository.findByUuid(this.exerciseUuid)
            ?: throw RuntimeException("Exercise not found: ${this.exerciseUuid}")

        return LiftingPRDTO(
            uuid = this.uuid,
            runnerId = this.runnerId,
            exercise = exercise.toDTO(),
            prType = this.prType,
            weight = this.weight,
            weightUnit = this.weightUnit,
            repNumber = this.repNumber,
            duration = this.duration,
            achievedDate = this.achievedDate,
            liftingRecordUuid = this.liftingRecordUuid
        )
    }
}
