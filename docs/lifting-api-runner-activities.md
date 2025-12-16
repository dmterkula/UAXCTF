# Lifting API - Runner Activities Endpoint

## Overview
This endpoint retrieves all lifting activities for a specific runner within a date range, including both completed and incomplete activities.

## Endpoint

### Get Runner Lifting Activities
**GET** `/api/lifting/runner/{runnerId}/activities`

Retrieves all lifting activities within a specified date range for a given runner, showing which activities they completed and which they didn't.

#### Path Parameters
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `runnerId` | Integer | Yes | The unique identifier of the runner |

#### Query Parameters
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `startDate` | Timestamp | Yes | Start date/time for the query range (ISO 8601 format) |
| `endDate` | Timestamp | Yes | End date/time for the query range (ISO 8601 format) |

#### Response Structure

```json
{
  "runnerId": 123,
  "startDate": "2025-01-01T00:00:00.000Z",
  "endDate": "2025-12-31T23:59:59.999Z",
  "activities": [
    {
      "activity": {
        "uuid": "act-uuid-1",
        "name": "Upper Body Strength",
        "description": "Focus on chest and triceps",
        "date": "2025-03-15",
        "duration": "45min",
        "icon": "dumbbell",
        "season": "Spring 2025",
        "team": "XC",
        "suggestedExercises": ["bench-press-uuid", "tricep-dip-uuid"]
      },
      "record": {
        "uuid": "rec-uuid-1",
        "liftingActivityUuid": "act-uuid-1",
        "runnerId": 123,
        "exercises": [...],
        "totalDuration": "50min",
        "notes": "Felt strong today",
        "coachesNotes": null,
        "effortLevel": 8.5,
        "dateLogged": "2025-03-15T10:30:00.000Z"
      },
      "recordExists": true
    },
    {
      "activity": {
        "uuid": "act-uuid-2",
        "name": "Core Workout",
        "description": "Planks and stability",
        "date": "2025-03-10",
        "duration": "30min",
        "icon": "core",
        "season": "Spring 2025",
        "team": "XC",
        "suggestedExercises": ["plank-uuid", "side-plank-uuid"]
      },
      "record": null,
      "recordExists": false
    }
  ],
  "totalActivities": 2,
  "completedActivities": 1
}
```

#### Response Fields

**Top Level:**
- `runnerId`: The runner's ID
- `startDate`: Query start date
- `endDate`: Query end date
- `activities`: Array of activity objects (sorted by date descending)
- `totalActivities`: Total number of activities in the date range
- `completedActivities`: Number of activities the runner logged

**Activity Object:**
- `activity`: The lifting activity details
  - `uuid`: Activity unique identifier
  - `name`: Activity name
  - `description`: Activity description
  - `date`: Scheduled date (ISO date string)
  - `duration`: Expected duration
  - `icon`: Icon identifier for UI
  - `season`: Season identifier
  - `team`: Team code
  - `suggestedExercises`: Array of suggested exercise UUIDs
- `record`: The runner's logged record (null if not completed)
  - Contains full exercise data, sets, reps, weights, durations
  - Includes notes, effort level, and timestamp
- `recordExists`: Boolean indicating if runner completed this activity

## Use Cases

1. **View Runner Progress**: See all scheduled activities and which ones were completed
2. **Identify Gaps**: Find activities a runner hasn't logged yet
3. **Generate Reports**: Create completion statistics for a time period
4. **Mobile App Display**: Show runner their upcoming and completed workouts

## Example Request

```bash
curl -X GET "http://localhost:8080/api/lifting/runner/123/activities?startDate=2025-01-01T00:00:00&endDate=2025-12-31T23:59:59" \
  -H "Accept: application/json"
```

## Notes

- Activities are sorted by date in descending order (most recent first)
- The endpoint returns ALL activities in the date range, not just completed ones
- Use `recordExists` to filter for completed vs incomplete activities
- Date parameters should be in ISO 8601 timestamp format
- Returns 404 if runner is not found
- Returns empty activities array if no activities exist in the date range
