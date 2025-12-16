# Exercise History API Implementation Plan

## Overview
Create a new API endpoint that shows a runner's recent logged exercises for a specific exercise type. This helps athletes see their recent performance data to plan their next workout and track progress.

## Requirements
- **Time-based filtering**: Query by date range (e.g., last 30/60/90 days)
- **Full details**: Return all sets, reps, weights, notes for each session
- **PR indicators**: Flag sessions where the athlete achieved a personal record
- **Context**: Include activity names and dates

---

## API Design

### Endpoint
```
GET /api/lifting/exercise-history
```

### Query Parameters
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `runnerId` | Integer | Yes | The runner's ID |
| `exerciseUuid` | String | Yes | UUID of the specific exercise |
| `startDate` | Timestamp | Yes | Start of date range (ISO 8601) |
| `endDate` | Timestamp | Yes | End of date range (ISO 8601) |

### Example Request
```
GET /api/lifting/exercise-history?runnerId=123&exerciseUuid=bench-press-uuid&startDate=2025-09-11T00:00:00&endDate=2025-12-11T23:59:59
```

---

## Response Structure

### New DTOs (add to LiftingResponses.kt)

```kotlin
data class ExerciseHistorySessionDTO(
    val sessionUuid: String,
    val activityName: String,
    val activityDate: String,
    val dateLogged: Timestamp,
    val exerciseEntry: LiftingExerciseEntryDTO,  // Full sets/reps/weights
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
    val sessions: List<ExerciseHistorySessionDTO>,  // Sorted by date desc
    val totalSessions: Int,
    val currentPR: LiftingPRDTO?
)
```

---

## Implementation Steps

### 1. Update Response DTOs
**File:** `src/main/kotlin/com/terkula/uaxctf/training/response/lifting/LiftingResponses.kt`

Add after existing PR response section (after line 106):
- `ExerciseHistorySessionDTO` - Represents one session
- `ExerciseHistoryResponse` - Complete history response

### 2. Implement Service Method
**File:** `src/main/kotlin/com/terkula/uaxctf/training/service/LiftingService.kt`

Add after `getRunnerPRs()` method (around line 410):

```kotlin
fun getExerciseHistory(
    runnerId: Int,
    exerciseUuid: String,
    startDate: Timestamp,
    endDate: Timestamp
): ExerciseHistoryResponse {
    // 1. Validate exercise and runner exist
    // 2. Query records by runner + date range
    // 3. Parse JSON and filter to records containing this exercise
    // 4. Batch load activities
    // 5. Get PRs to determine which sessions had PRs
    // 6. Transform to DTOs with activity context
    // 7. Sort by date descending (most recent first)
    // 8. Return response with current PR for context
}
```

**Algorithm:**
1. Validate exercise exists: `exerciseRepository.findByUuid(exerciseUuid)`
2. Validate runner exists: `runnerRepository.findById(runnerId)`
3. Query records: `liftingRecordRepository.findByRunnerIdAndDateLoggedBetween()`
4. Parse each record's JSON and extract matching exercise entries
5. Batch load activities: `liftingActivityRepository.findByUuid()` for each distinct activityUuid
6. Get all PRs: `liftingPRRepository.findByRunnerIdAndExerciseUuid()` to identify PR sessions
7. Map to `ExerciseHistorySessionDTO` with PR flags
8. Sort by `dateLogged` descending
9. Get current all-time PR for context
10. Return `ExerciseHistoryResponse`

### 3. Add Controller Endpoint
**File:** `src/main/kotlin/com/terkula/uaxctf/statistics/controller/LiftingController.kt`

Add after existing PR endpoints section (after line 160):

```kotlin
@ApiOperation("Get exercise history for a runner within a date range")
@RequestMapping(value = ["lifting/exercise-history"], method = [RequestMethod.GET])
fun getExerciseHistory(
    @ApiParam("Runner ID")
    @RequestParam(value = "runnerId", required = true) runnerId: Int,
    @ApiParam("Exercise UUID")
    @RequestParam(value = "exerciseUuid", required = true) exerciseUuid: String,
    @ApiParam("Start date")
    @RequestParam(value = "startDate", required = true) startDate: java.sql.Timestamp,
    @ApiParam("End date")
    @RequestParam(value = "endDate", required = true) endDate: java.sql.Timestamp
): ExerciseHistoryResponse {
    return liftingService.getExerciseHistory(runnerId, exerciseUuid, startDate, endDate)
}
```

---

## Key Implementation Details

### Filtering Logic
- Query all records in date range for runner
- Parse each record's `exercisesData` JSON
- Filter to records containing the specified `exerciseUuid`
- Extract only that exercise's data from each record

### PR Detection
- Query all PRs for runner + exercise
- Create set of record UUIDs that achieved PRs
- Flag each session with `isPR = true` if in PR set
- Include `prType` and `prValue` for PR sessions

### Activity Context
- Collect all unique `liftingActivityUuid` values from filtered records
- Batch load activities to get names and dates
- Avoid N+1 query problem by loading all at once

### Performance
- Typical 90-day query: ~20-30 records to parse
- JSON parsing overhead acceptable for this volume
- Response size: ~50-100KB for 90 days (mobile-friendly)

---

## Edge Cases

| Case | Behavior |
|------|----------|
| No records in date range | Return empty sessions list, totalSessions = 0 |
| Exercise never performed | Return empty sessions list |
| Invalid runner ID | Throw RuntimeException (404) |
| Invalid exercise UUID | Throw RuntimeException (404) |
| Activity deleted | Use "Unknown Activity" as fallback |
| No PRs for exercise | `currentPR` will be null |
| Multiple PRs in range | Flag each PR session |

---

## iOS App Use Cases

### 1. View Recent Progress
Show last 30 days of exercise history with PR indicators

### 2. Plan Next Workout
Display last session's weights/reps to guide next workout programming

### 3. Track Progressive Overload
Chart max weight over time to visualize progression

### 4. Quick Stats Summary
- Total sessions in period
- Current PR
- Last workout details
- Progression since start of period

---

## Example Response

```json
{
  "runnerId": 123,
  "exercise": {
    "uuid": "bench-press-uuid",
    "name": "Bench Press",
    "category": "UPPER_BODY",
    "exerciseType": "WEIGHT",
    "defaultWeightUnit": "lbs"
  },
  "startDate": "2025-09-11T00:00:00Z",
  "endDate": "2025-12-11T23:59:59Z",
  "sessions": [
    {
      "sessionUuid": "record-uuid-1",
      "activityName": "Upper Body Strength",
      "activityDate": "2025-12-09",
      "dateLogged": "2025-12-09T14:30:00Z",
      "exerciseEntry": {
        "sets": [
          {
            "setNumber": 1,
            "reps": [
              {"repNumber": 1, "weight": 225.0},
              {"repNumber": 2, "weight": 225.0}
            ]
          }
        ]
      },
      "sessionNotes": "Great workout",
      "exerciseNotes": "New PR!",
      "effortLevel": 8.5,
      "isPR": true,
      "prType": "MAX_WEIGHT",
      "prValue": "225.0 lbs"
    }
  ],
  "totalSessions": 12,
  "currentPR": {
    "weight": 225.0,
    "achievedDate": "2025-12-09T14:30:00Z"
  }
}
```

---

## Files to Modify

1. **LiftingResponses.kt** - Add `ExerciseHistorySessionDTO` and `ExerciseHistoryResponse`
2. **LiftingService.kt** - Add `getExerciseHistory()` method
3. **LiftingController.kt** - Add REST endpoint

## Files Already Have Required Methods
- **LiftingRecordRepository.kt** - Has `findByRunnerIdAndDateLoggedBetween()`
- **LiftingPRRepository.kt** - Has `findByRunnerIdAndExerciseUuid()`

---

## Testing Checklist

- [ ] Valid request returns exercise history
- [ ] Empty date range returns empty list
- [ ] Invalid runner ID returns 404
- [ ] Invalid exercise UUID returns 404
- [ ] PR flags are accurate
- [ ] Sessions sorted by date descending
- [ ] Activity context populated correctly
- [ ] 30/60/90 day queries perform well
