package com.terkula.uaxctf.training.response.lifting

import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.training.model.TrainingComment
import java.sql.Timestamp

// ===== Exercise Responses =====

data class ExerciseDTO(
    val uuid: String,
    val name: String,
    val description: String?,
    val category: String,
    val exerciseType: String,
    val isGlobal: Boolean,
    val team: String?,
    val createdBy: String?,
    val defaultWeightUnit: String
)

data class ExercisesResponse(
    val exercises: List<ExerciseDTO>
)

// ===== Activity Responses =====

data class LiftingActivityDTO(
    val uuid: String,
    val name: String,
    val description: String?,
    val date: String, // ISO date string
    val duration: String?,
    val icon: String,
    val season: String,
    val team: String,
    val suggestedExercises: List<String>?
)

data class LiftingActivitiesResponse(
    val activities: List<LiftingActivityDTO>
)

// ===== Record Responses =====

data class LiftingRepDTO(
    val uuid: String,
    val repNumber: Int,
    val weight: Double
)

data class LiftingSetDTO(
    val uuid: String,
    val setNumber: Int,
    val reps: List<LiftingRepDTO>,
    val duration: String?,
    val restTime: String?,
    val notes: String?
)

data class LiftingExerciseEntryDTO(
    val uuid: String,
    val exercise: ExerciseDTO,
    val sets: List<LiftingSetDTO>,
    val orderIndex: Int,
    val notes: String?
)

data class LiftingRecordDTO(
    val uuid: String,
    val liftingActivityUuid: String,
    val runnerId: Int,
    val exercises: List<LiftingExerciseEntryDTO>,
    val totalDuration: String?,
    val notes: String?,
    val coachesNotes: String?,
    val effortLevel: Double?,
    val dateLogged: Timestamp
)

data class LiftingRecordResponse(
    val runner: Runner,
    val liftingRecord: LiftingRecordDTO,
    val comments: List<TrainingComment>,
    val prsAchieved: List<LiftingPRDTO>
)

// ===== PR Responses =====

data class LiftingPRDTO(
    val uuid: String,
    val runnerId: Int,
    val exercise: ExerciseDTO,
    val prType: String, // MAX_WEIGHT or MAX_DURATION
    val weight: Double?,
    val weightUnit: String?,
    val repNumber: Int?,
    val duration: String?,
    val achievedDate: Timestamp,
    val liftingRecordUuid: String,
    val activityName: String? = null,
    val activityDate: String? = null
)

data class LiftingPRsResponse(
    val prs: List<LiftingPRDTO>
)

// ===== Exercise History Responses =====

data class ExerciseHistorySessionDTO(
    val sessionUuid: String,
    val activityName: String,
    val activityDate: String,
    val dateLogged: Timestamp,
    val exerciseEntry: LiftingExerciseEntryDTO,
    val sessionNotes: String?,
    val exerciseNotes: String?,
    val effortLevel: Double?,
    val isPR: Boolean,
    val prType: String? = null,
    val prValue: String? = null
)

data class ExerciseHistoryResponse(
    val runnerId: Int,
    val exercise: ExerciseDTO,
    val startDate: Timestamp,
    val endDate: Timestamp,
    val sessions: List<ExerciseHistorySessionDTO>,
    val totalSessions: Int,
    val currentPR: LiftingPRDTO?
)

// ===== Runner Activity Responses =====

data class RunnerLiftingActivityDTO(
    val activity: LiftingActivityDTO,
    val record: LiftingRecordDTO?,
    val recordExists: Boolean
)

data class RunnerLiftingActivitiesResponse(
    val runnerId: Int,
    val startDate: Timestamp,
    val endDate: Timestamp,
    val activities: List<RunnerLiftingActivityDTO>,
    val totalActivities: Int,
    val completedActivities: Int
)
