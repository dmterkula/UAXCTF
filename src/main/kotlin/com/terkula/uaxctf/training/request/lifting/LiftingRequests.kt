package com.terkula.uaxctf.training.request.lifting

import com.fasterxml.jackson.annotation.JsonFormat
import java.sql.Date
import java.sql.Timestamp

// ===== Exercise Requests =====

data class CreateExerciseRequest(
    val uuid: String,
    val name: String,
    val description: String?,
    val category: String, // UPPER_BODY, LOWER_BODY, CORE, FULL_BODY, PLYOMETRIC, FLEXIBILITY
    val exerciseType: String, // WEIGHT, BODYWEIGHT, DURATION
    val isGlobal: Boolean = false,
    val team: String?,
    val createdBy: String?,
    val defaultWeightUnit: String = "lbs"
)

data class UpdateExerciseRequest(
    val uuid: String,
    val name: String,
    val description: String?,
    val category: String,
    val exerciseType: String,
    val defaultWeightUnit: String
)

// ===== Activity Requests =====

data class CreateLiftingActivityRequest(
    val uuid: String,
    val name: String,
    val description: String?,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val date: Date,
    val duration: String?,
    val icon: String = "dumbbell",
    val season: String,
    val team: String,
    val suggestedExercises: List<String>? // Exercise UUIDs
)

data class UpdateLiftingActivityRequest(
    val uuid: String,
    val name: String,
    val description: String?,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val date: Date,
    val duration: String?,
    val icon: String,
    val season: String,
    val team: String,
    val suggestedExercises: List<String>?
)

// ===== Record Requests =====

data class LiftingRepRequest(
    val uuid: String,
    val repNumber: Int,
    val weight: Double
)

data class LiftingSetRequest(
    val uuid: String,
    val setNumber: Int,
    val reps: List<LiftingRepRequest>,
    val duration: String?,
    val restTime: String?,
    val notes: String?
)

data class LiftingExerciseEntryRequest(
    val uuid: String,
    val exerciseUuid: String,
    val sets: List<LiftingSetRequest>,
    val orderIndex: Int,
    val notes: String?
)

data class CreateLiftingRecordRequest(
    val uuid: String,
    val liftingActivityUuid: String,
    val runnerId: Int,
    val exercises: List<LiftingExerciseEntryRequest>,
    val totalDuration: String?,
    val notes: String?,
    val coachesNotes: String?,
    val effortLevel: Double?,
    val season: String?,
    val year: String?
)

data class UpdateLiftingRecordRequest(
    val uuid: String,
    val exercises: List<LiftingExerciseEntryRequest>,
    val totalDuration: String?,
    val notes: String?,
    val coachesNotes: String?,
    val effortLevel: Double?
)

// ===== Comment Request =====

data class CreateLiftingCommentRequest(
    val uuid: String,
    val liftingRecordUuid: String,
    val madeBy: String,
    val message: String,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss")
    val timestamp: Timestamp = Timestamp(System.currentTimeMillis())
)
