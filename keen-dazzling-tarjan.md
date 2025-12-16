# Backend Implementation Plan: Lifting/Strength Training Feature

## Overview

Implement complete backend API for the Lifting feature following the established CrossTraining pattern. This plan ensures exact alignment with front-end data models and leverages a hybrid database approach (normalized tables + JSON columns) for optimal performance and flexibility.

## Critical Requirements

### ✅ Front-End Contract Compliance
- **Exact attribute names**: Use camelCase in DTOs (uuid, exerciseType, isGlobal, liftingActivityUuid)
- **ExerciseType values**: WEIGHT, BODYWEIGHT, DURATION (tracks how sets/reps are logged)
- **ExerciseCategory values**: UPPER_BODY, LOWER_BODY, CORE, FULL_BODY, PLYOMETRIC, FLEXIBILITY
- **PR Types**: MAX_WEIGHT, MAX_DURATION (simplified approach)
- **Nested structures**: LiftingRecord contains exercises[] -> sets[] -> reps[]

### ✅ Backend Patterns to Follow
- **Architecture**: Spring Boot + Kotlin + JPA/Hibernate
- **Model Reference**: CrossTraining implementation (most similar feature)
- **Points Integration**: Award 10 points via PointsService.earnPoints() on first log
- **Comments**: Reuse existing TrainingComment entity
- **UUIDs**: Client-generated, stored as VARCHAR(255)

---

## 1. Database Schema

### Core Design Decision: Hybrid Approach

**Normalized Tables**: exercises, lifting_activities, lifting_records, lifting_prs
**JSON Columns**: Store sets/reps as JSON within lifting_records.exercises_data

**Rationale**:
- Exercises need filtering (global vs team-specific, by category)
- PRs need fast lookups for leaderboards
- Sets/reps are always retrieved together with the record (no separate queries needed)
- JSON provides flexibility for future enhancements

### Table 1: `exercises`

```sql
CREATE TABLE exercises (
    id INT PRIMARY KEY AUTO_INCREMENT,
    uuid VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    category VARCHAR(100) NOT NULL COMMENT 'UPPER_BODY, LOWER_BODY, CORE, FULL_BODY, PLYOMETRIC, FLEXIBILITY',
    exercise_type VARCHAR(50) NOT NULL COMMENT 'WEIGHT, BODYWEIGHT, DURATION',
    is_global BOOLEAN NOT NULL DEFAULT FALSE,
    team VARCHAR(20) NULL,
    created_by VARCHAR(100) NULL,
    default_weight_unit VARCHAR(10) NOT NULL DEFAULT 'lbs',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_global_category (is_global, category),
    INDEX idx_team_exercises (team, category),
    INDEX idx_uuid (uuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### Table 2: `lifting_activities`

```sql
CREATE TABLE lifting_activities (
    id INT PRIMARY KEY AUTO_INCREMENT,
    uuid VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    date DATE NOT NULL,
    duration VARCHAR(50) NULL,
    icon VARCHAR(50) NOT NULL DEFAULT 'dumbbell',
    season VARCHAR(20) NOT NULL,
    team VARCHAR(20) NOT NULL,
    suggested_exercises TEXT NULL COMMENT 'JSON array of exercise UUIDs',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_date_team (date, team),
    INDEX idx_season_team (season, team, date DESC),
    INDEX idx_uuid (uuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### Table 3: `lifting_records`

```sql
CREATE TABLE lifting_records (
    id INT PRIMARY KEY AUTO_INCREMENT,
    uuid VARCHAR(255) UNIQUE NOT NULL,
    lifting_activity_uuid VARCHAR(255) NOT NULL,
    runner_id INT NOT NULL,
    exercises_data TEXT NOT NULL COMMENT 'JSON: [{uuid, exerciseUuid, orderIndex, notes, sets: [...]}]',
    total_duration VARCHAR(50) NULL,
    notes TEXT NULL,
    coaches_notes TEXT NULL,
    effort_level DOUBLE NULL,
    date_logged TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_activity (lifting_activity_uuid),
    INDEX idx_runner (runner_id, date_logged DESC),
    INDEX idx_activity_runner (lifting_activity_uuid, runner_id),
    INDEX idx_uuid (uuid),
    FOREIGN KEY (runner_id) REFERENCES runners(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**exercises_data JSON Example**:
```json
[
  {
    "uuid": "entry-123",
    "exerciseUuid": "squat-uuid",
    "orderIndex": 0,
    "notes": "Felt strong",
    "sets": [
      {
        "uuid": "set-456",
        "setNumber": 1,
        "restTime": "90s",
        "duration": null,
        "notes": null,
        "reps": [
          {"uuid": "rep-1", "repNumber": 1, "weight": 185.0, "completed": true},
          {"uuid": "rep-2", "repNumber": 2, "weight": 185.0, "completed": true}
        ]
      }
    ]
  }
]
```

### Table 4: `lifting_prs`

```sql
CREATE TABLE lifting_prs (
    id INT PRIMARY KEY AUTO_INCREMENT,
    uuid VARCHAR(255) UNIQUE NOT NULL,
    runner_id INT NOT NULL,
    exercise_uuid VARCHAR(255) NOT NULL,
    pr_type VARCHAR(50) NOT NULL COMMENT 'MAX_WEIGHT or MAX_DURATION',
    weight DOUBLE NULL,
    weight_unit VARCHAR(10) NULL,
    rep_number INT NULL,
    duration VARCHAR(50) NULL,
    achieved_date TIMESTAMP NOT NULL,
    lifting_record_uuid VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_runner_exercise (runner_id, exercise_uuid, pr_type),
    INDEX idx_runner_date (runner_id, achieved_date DESC),
    INDEX idx_uuid (uuid),
    FOREIGN KEY (runner_id) REFERENCES runners(id),
    UNIQUE KEY unique_pr (runner_id, exercise_uuid, pr_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### Seed Data

```sql
INSERT INTO exercises (uuid, name, description, category, exercise_type, is_global, default_weight_unit) VALUES
    (UUID(), 'Barbell Back Squat', 'Traditional back squat', 'LOWER_BODY', 'WEIGHT', TRUE, 'lbs'),
    (UUID(), 'Barbell Bench Press', 'Flat bench press', 'UPPER_BODY', 'WEIGHT', TRUE, 'lbs'),
    (UUID(), 'Deadlift', 'Conventional deadlift', 'LOWER_BODY', 'WEIGHT', TRUE, 'lbs'),
    (UUID(), 'Pull-ups', 'Bodyweight pull-ups', 'UPPER_BODY', 'BODYWEIGHT', TRUE, 'lbs'),
    (UUID(), 'Romanian Deadlift', 'RDL variation', 'LOWER_BODY', 'WEIGHT', TRUE, 'lbs'),
    (UUID(), 'Overhead Press', 'Standing press', 'UPPER_BODY', 'WEIGHT', TRUE, 'lbs'),
    (UUID(), 'Bulgarian Split Squat', 'Single leg squat', 'LOWER_BODY', 'WEIGHT', TRUE, 'lbs'),
    (UUID(), 'Plank Hold', 'Static core hold', 'CORE', 'DURATION', TRUE, 'lbs'),
    (UUID(), 'Box Jumps', 'Plyometric jumps', 'PLYOMETRIC', 'BODYWEIGHT', TRUE, 'lbs'),
    (UUID(), 'Yoga Flow', 'Dynamic stretching', 'FLEXIBILITY', 'DURATION', TRUE, 'lbs');

INSERT INTO points_configuration (config_key, config_value, description) VALUES
    ('EARN_LIFTING', 10, 'Points awarded for logging a lifting session');
```

---

## 2. JPA Entities

### Exercise.kt
**Location**: `/src/main/kotlin/com/terkula/uaxctf/training/model/lifting/Exercise.kt`

```kotlin
@Entity
@Table(name = "exercises", schema = "uaxc")
@JsonInclude(JsonInclude.Include.NON_NULL)
class Exercise(
    @Column(name = "uuid", unique = true, nullable = false)
    var uuid: String,

    @Column(name = "name", nullable = false)
    var name: String,

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String?,

    @Column(name = "category", nullable = false)
    var category: String, // UPPER_BODY, LOWER_BODY, CORE, FULL_BODY, PLYOMETRIC, FLEXIBILITY

    @Column(name = "exercise_type", nullable = false)
    var exerciseType: String, // WEIGHT, BODYWEIGHT, DURATION

    @Column(name = "is_global", nullable = false)
    var isGlobal: Boolean = false,

    @Column(name = "team")
    var team: String?,

    @Column(name = "created_by")
    var createdBy: String?,

    @Column(name = "default_weight_unit", nullable = false)
    var defaultWeightUnit: String = "lbs",

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Timestamp = Timestamp(System.currentTimeMillis())
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    val id: Int = 0
}
```

### LiftingActivity.kt
**Location**: `/src/main/kotlin/com/terkula/uaxctf/training/model/lifting/LiftingActivity.kt`

```kotlin
@Entity
@Table(name = "lifting_activities", schema = "uaxc")
@JsonInclude(JsonInclude.Include.NON_NULL)
class LiftingActivity(
    @Column(name = "uuid", unique = true, nullable = false)
    var uuid: String,

    @Column(name = "name", nullable = false)
    var name: String,

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String?,

    @Column(name = "date", nullable = false)
    var date: Date,

    @Column(name = "duration")
    var duration: String?,

    @Column(name = "icon", nullable = false)
    var icon: String = "dumbbell",

    @Column(name = "season", nullable = false)
    var season: String,

    @Column(name = "team", nullable = false)
    var team: String,

    @Column(name = "suggested_exercises", columnDefinition = "TEXT")
    var suggestedExercises: String?, // JSON array

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Timestamp = Timestamp(System.currentTimeMillis()),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Timestamp = Timestamp(System.currentTimeMillis())
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    val id: Int = 0
}
```

### LiftingRecord.kt
**Location**: `/src/main/kotlin/com/terkula/uaxctf/training/model/lifting/LiftingRecord.kt`

```kotlin
@Entity
@Table(name = "lifting_records", schema = "uaxc")
@JsonInclude(JsonInclude.Include.NON_NULL)
class LiftingRecord(
    @Column(name = "uuid", unique = true, nullable = false)
    var uuid: String,

    @Column(name = "lifting_activity_uuid", nullable = false)
    var liftingActivityUuid: String,

    @Column(name = "runner_id", nullable = false)
    var runnerId: Int,

    @Column(name = "exercises_data", columnDefinition = "TEXT", nullable = false)
    var exercisesData: String, // JSON

    @Column(name = "total_duration")
    var totalDuration: String?,

    @Column(name = "notes", columnDefinition = "TEXT")
    var notes: String?,

    @Column(name = "coaches_notes", columnDefinition = "TEXT")
    var coachesNotes: String?,

    @Column(name = "effort_level")
    var effortLevel: Double?,

    @Column(name = "date_logged", nullable = false)
    var dateLogged: Timestamp = Timestamp(System.currentTimeMillis()),

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Timestamp = Timestamp(System.currentTimeMillis()),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Timestamp = Timestamp(System.currentTimeMillis())
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    val id: Int = 0
}
```

### LiftingPR.kt
**Location**: `/src/main/kotlin/com/terkula/uaxctf/training/model/lifting/LiftingPR.kt`

```kotlin
@Entity
@Table(
    name = "lifting_prs",
    schema = "uaxc",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["runner_id", "exercise_uuid", "pr_type"])
    ]
)
@JsonInclude(JsonInclude.Include.NON_NULL)
class LiftingPR(
    @Column(name = "uuid", unique = true, nullable = false)
    var uuid: String,

    @Column(name = "runner_id", nullable = false)
    var runnerId: Int,

    @Column(name = "exercise_uuid", nullable = false)
    var exerciseUuid: String,

    @Column(name = "pr_type", nullable = false)
    var prType: String, // MAX_WEIGHT or MAX_DURATION

    @Column(name = "weight")
    var weight: Double?,

    @Column(name = "weight_unit")
    var weightUnit: String?,

    @Column(name = "rep_number")
    var repNumber: Int?,

    @Column(name = "duration")
    var duration: String?,

    @Column(name = "achieved_date", nullable = false)
    var achievedDate: Timestamp,

    @Column(name = "lifting_record_uuid", nullable = false)
    var liftingRecordUuid: String,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Timestamp = Timestamp(System.currentTimeMillis())
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    val id: Int = 0
}
```

---

## 3. Repositories

### Location: `/src/main/kotlin/com/terkula/uaxctf/training/repository/`

**ExerciseRepository.kt**
```kotlin
@Repository
interface ExerciseRepository : CrudRepository<Exercise, Int> {
    fun findByUuid(uuid: String): Exercise?
    fun findByIsGlobalTrue(): List<Exercise>
    fun findByTeamAndIsGlobalFalse(team: String): List<Exercise>
    fun findByIsGlobalTrueOrTeam(team: String?): List<Exercise>
}
```

**LiftingActivityRepository.kt**
```kotlin
@Repository
interface LiftingActivityRepository : CrudRepository<LiftingActivity, Int> {
    fun findByUuid(uuid: String): LiftingActivity?
    fun findByDateBetween(startDate: Date, endDate: Date): List<LiftingActivity>
    fun findByDateBetweenAndTeam(startDate: Date, endDate: Date, team: String): List<LiftingActivity>
}
```

**LiftingRecordRepository.kt**
```kotlin
@Repository
interface LiftingRecordRepository : CrudRepository<LiftingRecord, Int> {
    fun findByUuid(uuid: String): LiftingRecord?
    fun findByLiftingActivityUuid(activityUuid: String): List<LiftingRecord>
    fun findByLiftingActivityUuidAndRunnerId(activityUuid: String, runnerId: Int): LiftingRecord?
    fun findByRunnerIdOrderByDateLoggedDesc(runnerId: Int): List<LiftingRecord>
}
```

**LiftingPRRepository.kt**
```kotlin
@Repository
interface LiftingPRRepository : CrudRepository<LiftingPR, Int> {
    fun findByRunnerId(runnerId: Int): List<LiftingPR>
    fun findByRunnerIdAndExerciseUuid(runnerId: Int, exerciseUuid: String): List<LiftingPR>
    fun findByRunnerIdAndExerciseUuidAndPrType(runnerId: Int, exerciseUuid: String, prType: String): LiftingPR?
}
```

---

## 4. Service Layer

### LiftingService.kt
**Location**: `/src/main/kotlin/com/terkula/uaxctf/training/service/LiftingService.kt`

#### Key Methods:

**Exercise Management**
- `getExercises(team: String)` - Returns global + team-specific exercises
- `createExercise(request)` - Creates new exercise
- `updateExercise(request)` - Updates existing exercise
- `deleteExercise(uuid)` - Deletes exercise (validates no PRs exist)

**Activity Management**
- `getLiftingActivities(startDate, endDate, team)` - Date range query
- `createLiftingActivity(request)` - Creates activity
- `updateLiftingActivity(request)` - Updates activity
- `deleteLiftingActivity(uuid)` - Validates no records exist

**Record Management**
- `createLiftingRecord(request)` - Creates record, awards points, calculates PRs
- `updateLiftingRecord(request)` - Updates record, recalculates PRs
- `getLiftingRecord(uuid)` - Single record with comments and PRs
- `getLiftingRecordsForActivity(activityUuid)` - All records for activity
- `getRunnerLiftingRecord(runnerId, activityUuid)` - Specific runner's record

**PR Calculation Logic**
```kotlin
private fun calculateAndSavePRs(
    record: LiftingRecord,
    exercises: List<LiftingExerciseEntryDTO>
): List<LiftingPRDTO> {
    val prsAchieved = mutableListOf<LiftingPRDTO>()

    exercises.forEach { exerciseEntry ->
        val exercise = exerciseRepository.findByUuid(exerciseEntry.exercise.uuid) ?: return@forEach

        when (exercise.exerciseType) {
            "WEIGHT", "BODYWEIGHT" -> {
                // Find max single rep weight across all sets
                val maxWeight = exerciseEntry.sets
                    .flatMap { it.reps }
                    .filter { it.completed }
                    .maxByOrNull { it.weight }?.weight ?: return@forEach

                val existingPR = liftingPRRepository.findByRunnerIdAndExerciseUuidAndPrType(
                    record.runnerId, exercise.uuid, "MAX_WEIGHT"
                )

                if (existingPR == null || maxWeight > (existingPR.weight ?: 0.0)) {
                    existingPR?.let { liftingPRRepository.delete(it) }

                    val newPR = LiftingPR(
                        uuid = UUID.randomUUID().toString(),
                        runnerId = record.runnerId,
                        exerciseUuid = exercise.uuid,
                        prType = "MAX_WEIGHT",
                        weight = maxWeight,
                        weightUnit = exercise.defaultWeightUnit,
                        repNumber = 1,
                        duration = null,
                        achievedDate = record.dateLogged,
                        liftingRecordUuid = record.uuid
                    )
                    prsAchieved.add(liftingPRRepository.save(newPR).toDTO())
                }
            }

            "DURATION" -> {
                // Find longest duration across all sets
                val maxDuration = exerciseEntry.sets
                    .mapNotNull { it.duration }
                    .maxByOrNull { parseDuration(it) } ?: return@forEach

                val existingPR = liftingPRRepository.findByRunnerIdAndExerciseUuidAndPrType(
                    record.runnerId, exercise.uuid, "MAX_DURATION"
                )

                if (existingPR == null || parseDuration(maxDuration) > parseDuration(existingPR.duration ?: "0")) {
                    existingPR?.let { liftingPRRepository.delete(it) }

                    val newPR = LiftingPR(
                        uuid = UUID.randomUUID().toString(),
                        runnerId = record.runnerId,
                        exerciseUuid = exercise.uuid,
                        prType = "MAX_DURATION",
                        weight = null,
                        weightUnit = null,
                        repNumber = null,
                        duration = maxDuration,
                        achievedDate = record.dateLogged,
                        liftingRecordUuid = record.uuid
                    )
                    prsAchieved.add(liftingPRRepository.save(newPR).toDTO())
                }
            }
        }
    }

    return prsAchieved
}

private fun parseDuration(duration: String): Long {
    // Parse "MM:SS" to seconds
    val parts = duration.split(":")
    return if (parts.size == 2) {
        (parts[0].toLongOrNull() ?: 0) * 60 + (parts[1].toLongOrNull() ?: 0)
    } else {
        0
    }
}
```

**Points Integration**
```kotlin
// In createLiftingRecord(), after saving record:
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
```

**JSON Parsing Helper**
```kotlin
private fun parseRecordToDTO(record: LiftingRecord): LiftingRecordDTO {
    val exerciseEntries: List<LiftingExerciseEntryRequest> =
        objectMapper.readValue(record.exercisesData)

    val exerciseDTOs = exerciseEntries.map { entry ->
        val exercise = exerciseRepository.findByUuid(entry.exerciseUuid)
            ?: throw RuntimeException("Exercise not found: ${entry.exerciseUuid}")

        LiftingExerciseEntryDTO(
            uuid = entry.uuid,
            exercise = exercise.toDTO(),
            sets = entry.sets.map { it.toDTO() },
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
```

---

## 5. Controller

### LiftingController.kt
**Location**: `/src/main/kotlin/com/terkula/uaxctf/training/controller/LiftingController.kt`

**Endpoint Mapping** (exact match to front-end spec):

```kotlin
@RestController
@RequestMapping("/api")
class LiftingController(private val liftingService: LiftingService) {

    // Exercises
    @GetMapping("/exercises")
    fun getExercises(@RequestParam team: String): ExercisesResponse

    @PostMapping("/exercises")
    fun createExercise(@RequestBody @Valid request: CreateExerciseRequest): ExerciseDTO

    @PutMapping("/exercises/{uuid}")
    fun updateExercise(@PathVariable uuid: String, @RequestBody request: UpdateExerciseRequest): ExerciseDTO

    @DeleteMapping("/exercises/{uuid}")
    fun deleteExercise(@PathVariable uuid: String)

    // Activities
    @GetMapping("/lifting/activities")
    fun getLiftingActivities(
        @RequestParam startDate: Date,
        @RequestParam endDate: Date,
        @RequestParam(required = false) team: String?
    ): LiftingActivitiesResponse

    @PostMapping("/lifting/activities")
    fun createLiftingActivity(@RequestBody request: CreateLiftingActivityRequest): LiftingActivityDTO

    @PutMapping("/lifting/activities/{uuid}")
    fun updateLiftingActivity(@PathVariable uuid: String, @RequestBody request: UpdateLiftingActivityRequest): LiftingActivityDTO

    @DeleteMapping("/lifting/activities/{uuid}")
    fun deleteLiftingActivity(@PathVariable uuid: String)

    // Records
    @GetMapping("/lifting/records")
    fun getLiftingRecords(
        @RequestParam(required = false) activityUuid: String?,
        @RequestParam(required = false) runnerId: Int?
    ): Any // Returns single record or list

    @PostMapping("/lifting/records")
    fun createLiftingRecord(@RequestBody request: CreateLiftingRecordRequest): LiftingRecordResponse

    @PutMapping("/lifting/records/{uuid}")
    fun updateLiftingRecord(@PathVariable uuid: String, @RequestBody request: UpdateLiftingRecordRequest): LiftingRecordResponse

    // PRs
    @GetMapping("/lifting/prs")
    fun getRunnerPRs(
        @RequestParam runnerId: Int,
        @RequestParam(required = false) exerciseUuid: String?
    ): LiftingPRsResponse

    // Comments
    @PostMapping("/lifting/comments")
    fun createComment(@RequestBody request: CreateLiftingCommentRequest): TrainingComment
}
```

---

## 6. Request/Response DTOs

### Location: `/src/main/kotlin/com/terkula/uaxctf/training/request/lifting/` and `/response/lifting/`

**Critical DTOs** (must match front-end exactly):

```kotlin
// Requests
data class CreateExerciseRequest(
    val uuid: String,
    val name: String,
    val description: String?,
    val category: String, // UPPER_BODY, LOWER_BODY, CORE, FULL_BODY, PLYOMETRIC, FLEXIBILITY
    val exerciseType: String, // WEIGHT, BODYWEIGHT, DURATION
    val isGlobal: Boolean,
    val team: String?,
    val defaultWeightUnit: String
)

data class LiftingRepRequest(
    val uuid: String,
    val repNumber: Int,
    val weight: Double,
    val completed: Boolean
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

// Responses
data class LiftingRecordResponse(
    val runner: Runner,
    val liftingRecord: LiftingRecordDTO,
    val comments: List<TrainingComment>,
    val prsAchieved: List<LiftingPRDTO>
)

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
    val liftingRecordUuid: String
)
```

---

## 7. Migration File

### File: `/db-migrations/add_lifting_feature.sql`

Complete SQL migration with:
- CREATE TABLE statements for all 4 tables
- Indexes for optimal query performance
- Seed data for 10-15 global exercises
- Points configuration entry

Execute via: `mysql -u root -p uaxc < db-migrations/add_lifting_feature.sql`

---

## 8. Implementation Sequence

### Week 1: Foundation (Days 1-3)
1. **Day 1**: Run migration, create all entity classes, test persistence
2. **Day 2**: Create repositories, create request/response DTOs
3. **Day 3**: Implement exercise and activity management in service layer

### Week 2: Core Features (Days 4-6)
4. **Day 4**: Implement record creation with JSON serialization
5. **Day 5**: Implement PR calculation logic (MAX_WEIGHT, MAX_DURATION)
6. **Day 6**: Integrate points system, test complete flow

### Week 3: API & Polish (Days 7-9)
7. **Day 7**: Create controller with all endpoints
8. **Day 8**: Integration testing, fix edge cases
9. **Day 9**: Performance testing, documentation

---

## 9. Critical Files Summary

**New Files to Create** (23 files):

### Entities (4)
- `/src/main/kotlin/com/terkula/uaxctf/training/model/lifting/Exercise.kt`
- `/src/main/kotlin/com/terkula/uaxctf/training/model/lifting/LiftingActivity.kt`
- `/src/main/kotlin/com/terkula/uaxctf/training/model/lifting/LiftingRecord.kt`
- `/src/main/kotlin/com/terkula/uaxctf/training/model/lifting/LiftingPR.kt`

### Repositories (4)
- `/src/main/kotlin/com/terkula/uaxctf/training/repository/ExerciseRepository.kt`
- `/src/main/kotlin/com/terkula/uaxctf/training/repository/LiftingActivityRepository.kt`
- `/src/main/kotlin/com/terkula/uaxctf/training/repository/LiftingRecordRepository.kt`
- `/src/main/kotlin/com/terkula/uaxctf/training/repository/LiftingPRRepository.kt`

### Service (1)
- `/src/main/kotlin/com/terkula/uaxctf/training/service/LiftingService.kt`

### Controller (1)
- `/src/main/kotlin/com/terkula/uaxctf/training/controller/LiftingController.kt`

### DTOs (2 files, multiple classes each)
- `/src/main/kotlin/com/terkula/uaxctf/training/request/lifting/LiftingRequests.kt`
- `/src/main/kotlin/com/terkula/uaxctf/training/response/lifting/LiftingResponses.kt`

### Migration (1)
- `/db-migrations/add_lifting_feature.sql`

**Files to Modify** (0 files):
- Points system already supports dynamic activity types via "LIFTING" string
- TrainingComment entity already supports any training type
- No enum updates needed in existing code

---

## 10. Testing Strategy

### Unit Tests
- Service layer: PR calculation with various weights/durations
- JSON serialization/deserialization of nested structures
- Points awarding (first log vs update)

### Integration Tests
- Complete record creation flow
- PR detection across multiple records
- Exercise library filtering (global vs team)
- Comment creation and retrieval

### API Tests (Postman/Swagger)
- All CRUD endpoints
- Complex nested record payloads
- Error handling (missing exercises, duplicate PRs)

---

## 11. Performance Considerations

### Database Indexes
- Composite index on (runner_id, exercise_uuid, pr_type) for fast PR lookups
- Index on (lifting_activity_uuid, runner_id) for record queries
- Index on (team, category) for exercise filtering

### Caching Opportunities
- Global exercises list (rarely changes)
- Runner's current PRs (invalidate on record update)
- Exercise library per team

### N+1 Query Prevention
- Batch load exercises when parsing multiple records
- Use repository methods that fetch related entities efficiently

---

## Success Criteria

✅ All API endpoints return exact data structures expected by front-end
✅ PR calculation correctly identifies MAX_WEIGHT and MAX_DURATION
✅ Points awarded on first lifting log (10 points)
✅ Comments integrate with existing TrainingComment system
✅ Exercise library supports global + team-specific exercises
✅ JSON nested structure (exercises -> sets -> reps) serializes correctly
✅ All attribute names match front-end camelCase expectations
✅ ExerciseType uses WEIGHT/BODYWEIGHT/DURATION values
✅ ExerciseCategory includes all 6 categories (UPPER_BODY, LOWER_BODY, CORE, FULL_BODY, PLYOMETRIC, FLEXIBILITY)
