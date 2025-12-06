# Seasonal Rewards System - iOS Integration Guide

## Overview

This guide details the backend API integration for the Seasonal Pride Points Rewards system. The backend implementation provides team-based seasonal rewards where all active roster members collectively work toward point thresholds.

**Key Principle**: Team Total Points = Sum of all active **UA team** roster members' seasonal points for a given season/year.

**Important**: Only **UA team** members are included in:
- Seasonal rewards team totals
- Pride points leaderboard
- Active roster counts

**NU team** members are excluded from these calculations.

---

## Deviations from Original Plan

### 1. Base URL Path
- **Original Plan**: `/api/points/seasonal/...`
- **Implemented**: `/api/v1/points/seasonal/...`
- **Impact**: Update all endpoint URLs to include `/v1/`

### 2. Reward ID Type
- **Original Plan**: UUID (String)
- **Implemented**: Long (numeric)
- **Impact**: Update iOS models to use `Int64` or `Long` instead of `String` for reward IDs

### 3. Season/Year Determination
- **Original Plan**: Backend determines season/year from date
- **Implemented**: Frontend passes both `season` and `year` explicitly
- **Impact**: iOS must determine current season and pass both values in requests
- **Benefit**: More explicit control, no timezone issues

### 4. Authentication Headers
- **Original Plan**: Full authentication with headers
- **Implemented**: Authentication TODOs in place but not enforced yet
- **Impact**: Endpoints are currently open for testing
- **Future**: Will require auth headers for coach-only endpoints (create/update/delete rewards)

### 5. Points Award Integration
- **Original Plan**: Backend auto-populates season from transaction date
- **Implemented**: Frontend must pass `year` field in addition to `season`
- **Impact**: Update all point-awarding API calls to include `year: String`

---

## API Endpoints

### Base URL
```
https://your-backend-domain.com/api/v1/points/seasonal
```

### 1. Get Team Progress

**Purpose**: Get team's total seasonal points, active roster count, and all rewards with progress percentages.

**Endpoint**: `GET /progress`

**Query Parameters**:
- `season` (required): `"xc"` or `"track"`
- `year` (required): String (e.g., `"2024"`)

**Example Request**:
```
GET /api/v1/points/seasonal/progress?season=xc&year=2024
```

**Response** (`SeasonalProgressResponse`):
```json
{
  "season": "xc",
  "year": 2024,
  "teamTotalPoints": 12500,
  "activeRosterCount": 45,  // Only counts UA team members
  "rewards": [
    {
      "id": 1,
      "season": "xc",
      "year": 2024,
      "description": "Team Pizza Party",
      "pointThreshold": 5000,
      "displayOrder": 1,
      "isAchieved": true,
      "achievedDate": "2024-10-15T10:30:00.000Z",
      "progressPercentage": 100.0
    },
    {
      "id": 2,
      "season": "xc",
      "year": 2024,
      "description": "Custom Team Hoodies",
      "pointThreshold": 10000,
      "displayOrder": 2,
      "isAchieved": true,
      "achievedDate": "2024-11-01T14:20:00.000Z",
      "progressPercentage": 100.0
    },
    {
      "id": 3,
      "season": "xc",
      "year": 2024,
      "description": "Team Trip to Indoor Track Meet",
      "pointThreshold": 20000,
      "displayOrder": 3,
      "isAchieved": false,
      "achievedDate": null,
      "progressPercentage": 62.5
    }
  ]
}
```

**iOS Usage**: Display on `TeamRewardsProgressView`

---

### 2. Get Seasonal Rewards Configuration

**Purpose**: Get all configured rewards for a season/year (without progress percentages).

**Endpoint**: `GET /rewards`

**Query Parameters**:
- `season` (required): `"xc"` or `"track"`
- `year` (required): String (e.g., `"2024"`)

**Example Request**:
```
GET /api/v1/points/seasonal/rewards?season=xc&year=2024
```

**Response** (`List<SeasonalRewardDTO>`):
```json
[
  {
    "id": 1,
    "season": "xc",
    "year": 2024,
    "description": "Team Pizza Party",
    "pointThreshold": 5000,
    "displayOrder": 1,
    "isAchieved": true,
    "achievedDate": "2024-10-15T10:30:00.000Z",
    "progressPercentage": null
  },
  {
    "id": 2,
    "season": "xc",
    "year": 2024,
    "description": "Custom Team Hoodies",
    "pointThreshold": 10000,
    "displayOrder": 2,
    "isAchieved": false,
    "achievedDate": null,
    "progressPercentage": null
  }
]
```

**iOS Usage**: Display on `SeasonalRewardsConfigView` (coaches only)

---

### 3. Create Seasonal Reward (Coach Only)

**Purpose**: Create a new reward tier for a season.

**Endpoint**: `POST /rewards`

**Authentication**: ⚠️ Currently not enforced, but will require coach authentication in future.

**Request Body** (`CreateSeasonalRewardRequest`):
```json
{
  "season": "xc",
  "year": 2024,
  "description": "Team Hoodies",
  "pointThreshold": 10000,
  "displayOrder": 2,
  "createdBy": "Coach Smith"
}
```

**Request Fields**:
- `season` (required): `"xc"` or `"track"`
- `year` (required): String (e.g., `"2024"`)
- `description` (required): String - Reward description
- `pointThreshold` (required): Integer - Points needed to unlock
- `displayOrder` (required): Integer - Display order (1, 2, 3...)
- `createdBy` (optional): String - Coach name

**Response** (`SeasonalReward`):
```json
{
  "id": 2,
  "season": "xc",
  "year": 2024,
  "description": "Team Hoodies",
  "pointThreshold": 10000,
  "displayOrder": 2,
  "isAchieved": false,
  "achievedDate": null,
  "createdAt": "2024-09-01T10:00:00.000Z",
  "updatedAt": "2024-09-01T10:00:00.000Z"
}
```

**Error Responses**:
- `400 Bad Request`: Display order already exists for this season/year
- `401 Unauthorized`: (Future) Not a coach

**iOS Usage**: `SeasonalRewardsConfigView` - Add reward form

---

### 4. Update Seasonal Reward (Coach Only)

**Purpose**: Update an existing reward's description, threshold, or display order.

**Endpoint**: `PUT /rewards/{id}`

**Path Parameters**:
- `id`: Reward ID (Long/Int64)

**Request Body** (`UpdateSeasonalRewardRequest`):
```json
{
  "description": "Custom Team Hoodies (Updated)",
  "pointThreshold": 12000,
  "displayOrder": 2
}
```

**Request Fields** (all optional):
- `description`: String - Updated description
- `pointThreshold`: Integer - Updated threshold
- `displayOrder`: Integer - Updated display order

**Response**: Same as Create (SeasonalReward entity)

**Error Responses**:
- `400 Bad Request`: Display order conflict
- `404 Not Found`: Reward ID doesn't exist

**iOS Usage**: `SeasonalRewardsConfigView` - Edit reward form

---

### 5. Delete Seasonal Reward (Coach Only)

**Purpose**: Delete a reward configuration.

**Endpoint**: `DELETE /rewards/{id}`

**Path Parameters**:
- `id`: Reward ID (Long/Int64)

**Response**: `200 OK` (empty body)

**Error Responses**:
- `404 Not Found`: Reward ID doesn't exist

**iOS Usage**: `SeasonalRewardsConfigView` - Delete confirmation dialog

---

### 6. Get Runner's Seasonal Points

**Purpose**: Get individual runner's points earned for a specific season/year.

**Endpoint**: `GET /{runnerId}`

**Path Parameters**:
- `runnerId`: Runner ID (Integer)

**Query Parameters**:
- `season` (required): `"xc"` or `"track"`
- `year` (required): String (e.g., `"2024"`)

**Example Request**:
```
GET /api/v1/points/seasonal/123?season=xc&year=2024
```

**Response** (`RunnerSeasonalPointsResponse`):
```json
{
  "runnerId": 123,
  "season": "xc",
  "year": 2024,
  "pointsEarned": 285
}
```

**iOS Usage**: Profile view, individual stats

---

## Modified Endpoint: Award Points

### Important Change to Existing Points API

**Endpoint**: `POST /api/v1/points/earn` (existing endpoint)

**CHANGE REQUIRED**: Add `year` field to request body.

**Updated Request Body** (`EarnPointsRequest`):
```json
{
  "runnerId": 123,
  "activityType": "TRAINING_RUN",
  "activityUuid": "550e8400-e29b-41d4-a716-446655440000",
  "season": "xc",
  "year": 2024,
  "description": "Morning run - 5 miles"
}
```

**New Field**:
- `year` (optional but recommended): String (e.g., `"2024"`) - Season year for seasonal tracking

**Behavior**:
- If both `season` and `year` are provided, the backend will:
  1. Award points as normal
  2. Update `seasonal_points_tracking` table
  3. Auto-check if any rewards should be achieved
- If `season` or `year` is null, seasonal tracking is skipped (backward compatible)

**iOS Integration Points**:
Update these service calls to include `year`:
- `TrainingRunsService` - when creating training runs
- `MeetLogService` - when logging meet performances
- `CrossTrainingService` - when logging cross training
- `WorkoutSplitService` - when logging workout results

---

## iOS Data Models

### 1. SeasonalRewardDTO

```swift
struct SeasonalRewardDTO: Codable, Identifiable, Equatable {
    let id: Int64                    // CHANGED from String (UUID)
    let season: String               // "xc" or "track"
    let year: String
    let description: String
    let pointThreshold: Int
    let displayOrder: Int
    let isAchieved: Bool
    let achievedDate: Date?
    let progressPercentage: Double?  // Only present in progress endpoint

    enum CodingKeys: String, CodingKey {
        case id, season, year, description
        case pointThreshold, displayOrder, isAchieved, achievedDate
        case progressPercentage
    }
}
```

### 2. SeasonalProgressResponse

```swift
struct SeasonalProgressResponse: Codable {
    let season: String               // "xc" or "track"
    let year: String
    let teamTotalPoints: Int
    let activeRosterCount: Int
    let rewards: [SeasonalRewardDTO]

    enum CodingKeys: String, CodingKey {
        case season, year, teamTotalPoints, activeRosterCount, rewards
    }
}
```

### 3. RunnerSeasonalPointsResponse

```swift
struct RunnerSeasonalPointsResponse: Codable {
    let runnerId: Int
    let season: String               // "xc" or "track"
    let year: String
    let pointsEarned: Int

    enum CodingKeys: String, CodingKey {
        case runnerId, season, year, pointsEarned
    }
}
```

### 4. CreateSeasonalRewardRequest

```swift
struct CreateSeasonalRewardRequest: Codable {
    let season: String
    let year: String
    let description: String
    let pointThreshold: Int
    let displayOrder: Int
    let createdBy: String?

    enum CodingKeys: String, CodingKey {
        case season, year, description
        case pointThreshold, displayOrder, createdBy
    }
}
```

### 5. UpdateSeasonalRewardRequest

```swift
struct UpdateSeasonalRewardRequest: Codable {
    let description: String?
    let pointThreshold: Int?
    let displayOrder: Int?

    enum CodingKeys: String, CodingKey {
        case description, pointThreshold, displayOrder
    }
}
```

### 6. Updated EarnPointsRequest

```swift
struct EarnPointsRequest: Codable {
    let runnerId: Int
    let activityType: String
    let activityUuid: String?
    let season: String?
    let year: String?                // NEW FIELD
    let description: String?

    enum CodingKeys: String, CodingKey {
        case runnerId, activityType, activityUuid
        case season, year, description
    }
}
```

---

## DataService Methods

Add these methods to your `DataService.swift`:

```swift
// MARK: - Seasonal Rewards API

/// Get team progress for a season/year
func getTeamSeasonalProgress(season: String, year: String) async -> Result<SeasonalProgressResponse, Error> {
    let endpoint = "\(baseUrl)/api/v1/points/seasonal/progress?season=\(season)&year=\(year)"
    return await performGETRequest(url: endpoint)
}

/// Get seasonal rewards configuration
func getSeasonalRewardsConfig(season: String, year: String) async -> Result<[SeasonalRewardDTO], Error> {
    let endpoint = "\(baseUrl)/api/v1/points/seasonal/rewards?season=\(season)&year=\(year)"
    return await performGETRequest(url: endpoint)
}

/// Create a new seasonal reward (coach only)
func createSeasonalReward(request: CreateSeasonalRewardRequest) async -> Result<SeasonalRewardDTO, Error> {
    let endpoint = "\(baseUrl)/api/v1/points/seasonal/rewards"
    return await performPOSTRequest(url: endpoint, body: request)
}

/// Update an existing reward (coach only)
func updateSeasonalReward(id: Int64, request: UpdateSeasonalRewardRequest) async -> Result<SeasonalRewardDTO, Error> {
    let endpoint = "\(baseUrl)/api/v1/points/seasonal/rewards/\(id)"
    return await performPUTRequest(url: endpoint, body: request)
}

/// Delete a reward (coach only)
func deleteSeasonalReward(id: Int64) async -> Result<Void, Error> {
    let endpoint = "\(baseUrl)/api/v1/points/seasonal/rewards/\(id)"
    return await performDELETERequest(url: endpoint)
}

/// Get individual runner's seasonal points
func getRunnerSeasonalPoints(runnerId: Int, season: String, year: String) async -> Result<RunnerSeasonalPointsResponse, Error> {
    let endpoint = "\(baseUrl)/api/v1/points/seasonal/\(runnerId)?season=\(season)&year=\(year)"
    return await performGETRequest(url: endpoint)
}
```

---

## Season Determination Logic (iOS)

Since the backend now requires the iOS app to pass both `season` and `year`, implement this helper:

```swift
extension GlobalFunctions {
    /// Determines the current season and year based on the date.
    /// XC Season: June 2 - Nov 18 (uses current year)
    /// Track Season: Nov 18 - June 2 (spans two calendar years, uses starting year)
    static func getCurrentSeasonAndYear() -> (season: String, year: String) {
        let now = Date()
        let calendar = Calendar.current
        let month = calendar.component(.month, from: now)
        let day = calendar.component(.day, from: now)
        let currentYear = calendar.component(.year, from: now)

        // XC Season: June 2 - Nov 18
        if (month == 6 && day >= 2) || (month >= 7 && month <= 10) || (month == 11 && day < 18) {
            return ("xc", String(currentYear))
        }
        // Track Season: Nov 18 - Dec 31 (current year)
        else if month == 11 && day >= 18 || month == 12 {
            return ("track", String(currentYear))
        }
        // Track Season: Jan 1 - June 1 (previous year)
        else {
            return ("track", String(currentYear - 1))
        }
    }
}

// Usage:
let (season, year) = GlobalFunctions.getCurrentSeasonAndYear()
// season = "xc" or "track"
// year = "2024" (string)
```

---

## Integration Checklist

### Phase 1: Update Existing Points Integration
- [ ] Add `year: String?` field to `EarnPointsRequest` model
- [ ] Implement `getCurrentSeasonAndYear()` helper function
- [ ] Update all point-awarding calls to include `year`:
  - [ ] Training runs
  - [ ] Meet logs
  - [ ] Pre-meet logs
  - [ ] Cross training
  - [ ] Workout splits

### Phase 2: Create Data Models
- [ ] Create `SeasonalRewardDTO` (change ID type to `Int64`)
- [ ] Create `SeasonalProgressResponse`
- [ ] Create `RunnerSeasonalPointsResponse`
- [ ] Create `CreateSeasonalRewardRequest`
- [ ] Create `UpdateSeasonalRewardRequest`

### Phase 3: Add DataService Methods
- [ ] `getTeamSeasonalProgress()`
- [ ] `getSeasonalRewardsConfig()`
- [ ] `createSeasonalReward()`
- [ ] `updateSeasonalReward()`
- [ ] `deleteSeasonalReward()`
- [ ] `getRunnerSeasonalPoints()`

### Phase 4: Build UI Views
- [ ] `TeamRewardsProgressView` - Display team progress
- [ ] `RewardTierCard` component - Individual reward card with progress bar
- [ ] `SeasonalRewardsConfigView` - Coach configuration (full coaches only)
- [ ] `AddEditRewardView` - Add/edit reward form

### Phase 5: Navigation Integration
- [ ] Add Pride Points menu to `ContentView`
- [ ] Add navigation links to team progress
- [ ] Add navigation link to config (coaches only)
- [ ] Move existing leaderboard into Pride Points menu

### Phase 6: Testing
- [ ] Test team progress loads correctly
- [ ] Test progress bars display accurate percentages
- [ ] Test reward creation (coaches)
- [ ] Test reward update (coaches)
- [ ] Test reward deletion (coaches)
- [ ] Test auto-achievement (cross threshold and verify)
- [ ] Test season/year picker functionality
- [ ] Test with inactive runners (should not count toward total)

---

## Example Usage Flow

### 1. Display Team Progress

```swift
struct TeamRewardsProgressView: View {
    @EnvironmentObject var dataService: DataService
    @State private var progress: SeasonalProgressResponse?
    @State private var isLoading = false

    var body: some View {
        VStack {
            if let progress = progress {
                Text("\(progress.teamTotalPoints) Team Points")
                    .font(.largeTitle)
                    .fontWeight(.bold)

                Text("\(progress.activeRosterCount) Active UA Athletes")
                    .font(.subheadline)

                ForEach(progress.rewards) { reward in
                    RewardTierCard(reward: reward, teamPoints: progress.teamTotalPoints)
                }
            }
        }
        .onAppear {
            loadTeamProgress()
        }
    }

    func loadTeamProgress() {
        isLoading = true
        let (season, year) = GlobalFunctions.getCurrentSeasonAndYear()

        Task {
            let result = await dataService.getTeamSeasonalProgress(
                season: season,
                year: year
            )

            await MainActor.run {
                isLoading = false
                switch result {
                case .success(let data):
                    self.progress = data
                case .failure(let error):
                    print("Error loading progress: \(error)")
                }
            }
        }
    }
}
```

### 2. Award Points with Season/Year

```swift
// When creating a training run
func createTrainingRun(runnerId: Int, distance: Double) async {
    let uuid = UUID().uuidString
    let (season, year) = GlobalFunctions.getCurrentSeasonAndYear()

    // Create training run first
    // ...

    // Award points
    let earnRequest = EarnPointsRequest(
        runnerId: runnerId,
        activityType: "TRAINING_RUN",
        activityUuid: uuid,
        season: season,
        year: year,                    // NEW FIELD
        description: "Training run - \(distance) miles"
    )

    let result = await dataService.earnPoints(request: earnRequest)
    // Handle result
}
```

### 3. Create Reward (Coaches)

```swift
func createReward() async {
    let (season, year) = GlobalFunctions.getCurrentSeasonAndYear()

    let request = CreateSeasonalRewardRequest(
        season: season,
        year: year,
        description: "Team Pizza Party",
        pointThreshold: 5000,
        displayOrder: 1,
        createdBy: authentication.user?.username
    )

    let result = await dataService.createSeasonalReward(request: request)

    switch result {
    case .success(let reward):
        print("Created reward: \(reward.description)")
        // Refresh reward list
    case .failure(let error):
        print("Error: \(error)")
    }
}
```

---

## Error Handling

### Common Error Scenarios

1. **Display Order Conflict** (400 Bad Request)
   ```swift
   if response.statusCode == 400 {
       showAlert("A reward with this display order already exists. Please choose a different order.")
   }
   ```

2. **Reward Not Found** (404 Not Found)
   ```swift
   if response.statusCode == 404 {
       showAlert("This reward no longer exists. It may have been deleted.")
   }
   ```

3. **Empty Team** (200 OK but 0 points)
   ```swift
   if progress.activeRosterCount == 0 {
       showEmptyState("No active athletes on roster for this season")
   }
   ```

4. **No Rewards Configured** (200 OK but empty array)
   ```swift
   if progress.rewards.isEmpty {
       if isCoach {
           showEmptyState("No rewards configured. Tap + to create your first reward!")
       } else {
           showEmptyState("Check back soon - coaches will configure rewards for this season.")
       }
   }
   ```

---

## Performance Considerations

1. **Caching**: Consider caching team progress for 1-2 minutes to avoid excessive API calls
2. **Pagination**: Current implementation loads all rewards (no pagination needed for typical use)
3. **Real-time Updates**: Progress only updates when points are earned, no need for polling
4. **Pull-to-Refresh**: Implement on progress view to allow manual refresh

---

## Testing Endpoints

Use these curl commands to test the backend:

```bash
# 1. Get team progress
curl "https://your-backend.com/api/v1/points/seasonal/progress?season=xc&year=2024"

# 2. Create a reward
curl -X POST "https://your-backend.com/api/v1/points/seasonal/rewards" \
  -H "Content-Type: application/json" \
  -d '{
    "season": "xc",
    "year": 2024,
    "description": "Team Pizza Party",
    "pointThreshold": 1000,
    "displayOrder": 1,
    "createdBy": "Coach Smith"
  }'

# 3. Get runner's seasonal points
curl "https://your-backend.com/api/v1/points/seasonal/123?season=xc&year=2024"

# 4. Update a reward
curl -X PUT "https://your-backend.com/api/v1/points/seasonal/rewards/1" \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Team Pizza Party (Updated)",
    "pointThreshold": 1200
  }'

# 5. Delete a reward
curl -X DELETE "https://your-backend.com/api/v1/points/seasonal/rewards/1"
```

---

## Summary of Key Changes

| Aspect | Original Plan | Implemented |
|--------|--------------|-------------|
| Base URL | `/api/points/seasonal/` | `/api/v1/points/seasonal/` |
| Reward ID Type | String (UUID) | Long (Int64) |
| Season Determination | Backend (from date) | Frontend passes explicitly |
| Year in EarnPoints | Not required | Required for seasonal tracking |
| Authentication | Headers enforced | TODOs (not enforced yet) |
| Auto-Achievement | Manual coach approval | Automatic when threshold met |
| Team Filtering | Not specified | **Only UA team** (NU excluded) |

---

## Support

For questions or issues:
1. Check Swagger docs at: `https://your-backend.com/swagger-ui.html`
2. Review backend source code in: `/src/main/kotlin/com/terkula/uaxctf/statistics/`
3. Contact backend team for API changes or bug reports

---

**Last Updated**: December 4, 2024
**Backend Version**: 1.0.0
**Compatible iOS Version**: TBD