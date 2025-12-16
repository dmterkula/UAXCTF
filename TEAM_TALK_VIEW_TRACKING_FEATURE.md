# Team Talk View Tracking Feature

## Overview
Track user views on team talks to provide analytics for coaches. Every time a user loads a team talk page, a view record is created. View counts and viewer lists are returned with team talk responses for coach visibility.

## Requirements
- Track every view by authenticated users (not just unique views)
- Frontend makes API call on page load to track view
- Track using username (no foreign key constraint - soft reference)
- No tracking for unauthenticated users
- View counts returned in service layer responses
- No gamification (no pride points)
- Frontend: Only coaches see view counts/lists

---

## Database Schema

### New Table: `team_talk_views`

```sql
CREATE TABLE team_talk_views (
    id INT PRIMARY KEY AUTO_INCREMENT,
    team_talk_uuid VARCHAR(255) NOT NULL,
    username VARCHAR(255) NOT NULL COMMENT 'AppUser username who viewed (soft reference)',
    display_name VARCHAR(255) NOT NULL COMMENT 'Display name for UI (cached at view time)',
    viewed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_team_talk (team_talk_uuid, viewed_at DESC),
    INDEX idx_username (username),
    INDEX idx_team_talk_username (team_talk_uuid, username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Tracks all views of team talks by authenticated users';
```

**Migration File:** `db-migrations/add_team_talk_views.sql`

### Schema Design Decisions
- **No Foreign Key:** Per requirements, `username` is a soft reference (not constrained to `app_users.username`)
- **No Foreign Key on UUID:** `team_talk_uuid` is also a soft reference (not constrained to `team_talks.uuid`)
- **Display Name Cached:** Stored at view time to preserve name even if user's display name changes later
- **viewed_at:** Tracks when each view occurred for potential future analytics
- **Indexes:**
  - `idx_team_talk`: Efficient lookup of all views for a team talk (ordered by time)
  - `idx_username`: Efficient lookup of all views by a specific user
  - `idx_team_talk_username`: Efficient lookup for specific user's views on specific team talk

---

## Data Model

### Kotlin Entity: `TeamTalkView`

**File:** `src/main/kotlin/com/terkula/uaxctf/statisitcs/model/TeamTalkView.kt`

```kotlin
package com.terkula.uaxctf.statisitcs.model

import com.fasterxml.jackson.annotation.JsonIgnore
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "team_talk_views")
data class TeamTalkView(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    val id: Int = 0,

    @Column(name = "team_talk_uuid", nullable = false)
    val teamTalkUuid: String,

    @Column(nullable = false)
    val username: String,

    @Column(name = "display_name", nullable = false)
    val displayName: String,

    @Column(name = "viewed_at", nullable = false, updatable = false)
    val viewedAt: Timestamp = Timestamp(System.currentTimeMillis())
)
```

---

## Repository Layer

### Interface: `TeamTalkViewRepository`

**File:** `src/main/kotlin/com/terkula/uaxctf/statistics/repository/TeamTalkViewRepository.kt`

```kotlin
package com.terkula.uaxctf.statistics.repository

import com.terkula.uaxctf.statisitcs.model.TeamTalkView
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface TeamTalkViewRepository : CrudRepository<TeamTalkView, Int> {

    /**
     * Get all views for a specific team talk, ordered by most recent first
     */
    fun findByTeamTalkUuidOrderByViewedAtDesc(teamTalkUuid: String): List<TeamTalkView>

    /**
     * Count total views for a specific team talk
     */
    fun countByTeamTalkUuid(teamTalkUuid: String): Long

    /**
     * Get all views by a specific user for a specific team talk
     */
    fun findByTeamTalkUuidAndUsername(teamTalkUuid: String, username: String): List<TeamTalkView>

    /**
     * Count views by a specific user for a specific team talk
     */
    fun countByTeamTalkUuidAndUsername(teamTalkUuid: String, username: String): Long
}
```

### Repository Methods Explained
- **findByTeamTalkUuidOrderByViewedAtDesc:** Returns all view records for analytics/debugging
- **countByTeamTalkUuid:** Efficient count query for total view count
- **findByTeamTalkUuidAndUsername:** Get specific user's view history (for potential "you viewed this X times")
- **countByTeamTalkUuidAndUsername:** Count how many times a specific user viewed a team talk

---

## Request/Response DTOs

### Request: `TrackTeamTalkViewRequest`

**File:** `src/main/kotlin/com/terkula/uaxctf/statistics/request/TeamTalkRequests.kt`

**Add to existing file:**

```kotlin
data class TrackTeamTalkViewRequest(
    val teamTalkUuid: String,
    val username: String,
    val displayName: String
)
```

### Response: `ViewSummary`

**File:** `src/main/kotlin/com/terkula/uaxctf/statistics/response/TeamTalkResponses.kt`

**Add to existing file:**

```kotlin
/**
 * Summary of views for a team talk
 * Only exposed to coaches on frontend
 */
data class ViewSummary(
    val totalViews: Long,
    val uniqueViewers: Int,
    val recentViews: List<ViewDetail>  // Last 20 views
)

/**
 * Individual view detail
 */
data class ViewDetail(
    val username: String,
    val displayName: String,
    val viewedAt: String  // ISO 8601 string format for consistency with comments
)
```

### Updated: `TeamTalkResponse`

**Modify existing `TeamTalkResponse` in `TeamTalkResponses.kt`:**

```kotlin
data class TeamTalkResponse(
    val teamTalk: TeamTalk,
    val reactions: List<ReactionSummary>,
    val comments: List<NestedComment>,
    val totalCommentCount: Int,
    val viewSummary: ViewSummary  // NEW: Add view tracking data
)
```

---

## Service Layer

### Update: `TeamTalkService`

**File:** `src/main/kotlin/com/terkula/uaxctf/statistics/service/TeamTalkService.kt`

#### New Method: `trackView`

```kotlin
/**
 * Track a view of a team talk
 * Called every time a user loads the team talk page
 * No deduplication - tracks all views
 */
fun trackView(request: TrackTeamTalkViewRequest): TeamTalkView {
    val view = TeamTalkView(
        teamTalkUuid = request.teamTalkUuid,
        username = request.username,
        displayName = request.displayName
    )
    return teamTalkViewRepository.save(view)
}
```

#### New Method: `getViewSummary`

```kotlin
/**
 * Get view summary for a team talk
 * Returns total count, unique viewers, and recent view history
 */
fun getViewSummary(teamTalkUuid: String): ViewSummary {
    val allViews = teamTalkViewRepository.findByTeamTalkUuidOrderByViewedAtDesc(teamTalkUuid)
    val totalViews = allViews.size.toLong()
    val uniqueViewers = allViews.map { it.username }.toSet().size

    // Get last 20 views for recent activity
    val recentViews = allViews.take(20).map { view ->
        ViewDetail(
            username = view.username,
            displayName = view.displayName,
            viewedAt = view.viewedAt.toInstant().toString()  // ISO 8601 format
        )
    }

    return ViewSummary(
        totalViews = totalViews,
        uniqueViewers = uniqueViewers,
        recentViews = recentViews
    )
}
```

#### Update Existing Methods

**Modify `getTeamTalk` method:**

```kotlin
fun getTeamTalk(uuid: String): TeamTalkResponse {
    val teamTalk = teamTalkRepository.findByUuid(uuid)
        ?: throw IllegalArgumentException("Team talk not found")

    val reactions = getReactionSummary(uuid)
    val comments = getNestedComments(uuid)
    val totalCommentCount = countAllComments(comments)
    val viewSummary = getViewSummary(uuid)  // NEW: Add view summary

    return TeamTalkResponse(
        teamTalk = teamTalk,
        reactions = reactions,
        comments = comments,
        totalCommentCount = totalCommentCount,
        viewSummary = viewSummary  // NEW
    )
}
```

**Modify `getTeamTalksBySeason` and `getAllTeamTalks`:**

Both methods return `List<TeamTalkResponse>`, so each team talk in the list will include view summaries:

```kotlin
fun getTeamTalksBySeason(season: String, year: String, team: String): TeamTalkListResponse {
    val teamTalks = if (team.isBlank()) {
        teamTalkRepository.findBySeasonAndYearOrderByCreatedAtDesc(season, year)
    } else {
        teamTalkRepository.findBySeasonAndYearAndTeamOrderByCreatedAtDesc(season, year, team)
    }

    val responses = teamTalks.map { teamTalk ->
        TeamTalkResponse(
            teamTalk = teamTalk,
            reactions = getReactionSummary(teamTalk.uuid),
            comments = getNestedComments(teamTalk.uuid),
            totalCommentCount = countAllComments(getNestedComments(teamTalk.uuid)),
            viewSummary = getViewSummary(teamTalk.uuid)  // NEW
        )
    }

    return TeamTalkListResponse(teamTalks = responses)
}

fun getAllTeamTalks(): TeamTalkListResponse {
    val teamTalks = teamTalkRepository.findAllByOrderByCreatedAtDesc()

    val responses = teamTalks.map { teamTalk ->
        TeamTalkResponse(
            teamTalk = teamTalk,
            reactions = getReactionSummary(teamTalk.uuid),
            comments = getNestedComments(teamTalk.uuid),
            totalCommentCount = countAllComments(getNestedComments(teamTalk.uuid)),
            viewSummary = getViewSummary(teamTalk.uuid)  // NEW
        )
    }

    return TeamTalkListResponse(teamTalks = responses)
}
```

#### Service Constructor Update

**Add repository injection:**

```kotlin
@Service
class TeamTalkService(
    private val teamTalkRepository: TeamTalkRepository,
    private val teamTalkReactionRepository: TeamTalkReactionRepository,
    private val teamTalkCommentRepository: TeamTalkCommentRepository,
    private val teamTalkViewRepository: TeamTalkViewRepository,  // NEW
    private val authenticationRepository: AuthenticationRepository,
    private val pointsService: PointsService
) {
    // ... methods
}
```

---

## Controller Layer

### Update: `TeamTalkController`

**File:** `src/main/kotlin/com/terkula/uaxctf/statistics/controller/TeamTalkController.kt`

#### New Endpoint: Track View

```kotlin
/**
 * Track a view of a team talk
 * Called by frontend on page load
 *
 * POST /api/v1/team-talks/views/track
 */
@PostMapping("/views/track")
fun trackView(@RequestBody request: TrackTeamTalkViewRequest): ResponseEntity<Void> {
    teamTalkService.trackView(request)
    return ResponseEntity.ok().build()
}
```

### Endpoint Details

**URL:** `POST /api/v1/team-talks/views/track`

**Request Body:**
```json
{
  "teamTalkUuid": "abc-123-def-456",
  "username": "jdoe",
  "displayName": "John Doe"
}
```

**Response:**
- `200 OK` (empty body)
- `400 Bad Request` if validation fails

**Authentication:** Required (frontend must pass authenticated user's username/displayName)

---

## Frontend Integration Guide

### When to Call Track Endpoint

**On page load** of team talk detail view:

```typescript
// Example: When TeamTalkDetailPage mounts
useEffect(() => {
  const trackView = async () => {
    if (user && user.username) {  // Only for authenticated users
      await fetch('/api/v1/team-talks/views/track', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          teamTalkUuid: teamTalk.uuid,
          username: user.username,
          displayName: user.displayName
        })
      });
    }
  };

  trackView();
}, [teamTalk.uuid, user]);
```

### Displaying View Counts

**For Coaches Only:**

```typescript
// Example: Conditional rendering in TeamTalkDetail component
{user.role === 'coach' && (
  <ViewStats>
    <div>Total Views: {teamTalk.viewSummary.totalViews}</div>
    <div>Unique Viewers: {teamTalk.viewSummary.uniqueViewers}</div>

    <ViewerList>
      <h4>Recent Views:</h4>
      {teamTalk.viewSummary.recentViews.map(view => (
        <div key={`${view.username}-${view.viewedAt}`}>
          {view.displayName} - {formatDate(view.viewedAt)}
        </div>
      ))}
    </ViewerList>
  </ViewStats>
)}
```

**For Runners/Non-Coaches:**
- View tracking happens silently in background
- No UI elements displayed
- Counts remain hidden

---

## Implementation Checklist

### Database
- [ ] Create migration file `db-migrations/add_team_talk_views.sql`
- [ ] Run migration on development database
- [ ] Verify indexes created correctly
- [ ] Test query performance with sample data

### Backend - Model Layer
- [ ] Create `TeamTalkView.kt` entity
- [ ] Add `@Entity` and `@Table` annotations
- [ ] Verify field mappings (camelCase to snake_case)

### Backend - Repository Layer
- [ ] Create `TeamTalkViewRepository.kt` interface
- [ ] Implement query methods (findBy, countBy)
- [ ] Write repository tests (optional but recommended)

### Backend - Request/Response DTOs
- [ ] Add `TrackTeamTalkViewRequest` to `TeamTalkRequests.kt`
- [ ] Add `ViewSummary` and `ViewDetail` to `TeamTalkResponses.kt`
- [ ] Update `TeamTalkResponse` to include `viewSummary` field

### Backend - Service Layer
- [ ] Add `teamTalkViewRepository` to `TeamTalkService` constructor
- [ ] Implement `trackView()` method
- [ ] Implement `getViewSummary()` method
- [ ] Update `getTeamTalk()` to include view summary
- [ ] Update `getTeamTalksBySeason()` to include view summaries
- [ ] Update `getAllTeamTalks()` to include view summaries

### Backend - Controller Layer
- [ ] Add `POST /views/track` endpoint to `TeamTalkController`
- [ ] Add request validation
- [ ] Test endpoint with Postman/curl

### Testing
- [ ] Test view tracking endpoint (POST /views/track)
- [ ] Test view count accuracy (multiple views by same user)
- [ ] Test unique viewer count calculation
- [ ] Test recent views list (limited to 20)
- [ ] Test GET endpoints include view summaries
- [ ] Test with unauthenticated requests (should fail)

### Frontend
- [ ] Add view tracking API call on team talk page load
- [ ] Only call for authenticated users
- [ ] Display view counts for coaches only
- [ ] Display recent viewer list for coaches only
- [ ] Hide all view data from non-coach users
- [ ] Handle API errors gracefully (silent fail)

---

## Data Flow Diagram

```
User loads team talk page (authenticated)
    |
    v
Frontend calls POST /api/v1/team-talks/views/track
    |
    v
TeamTalkController.trackView()
    |
    v
TeamTalkService.trackView()
    |
    v
TeamTalkViewRepository.save() --> INSERT into team_talk_views
    |
    v
Return 200 OK to frontend

---

Coach loads team talk page
    |
    v
Frontend calls GET /api/v1/team-talks/{uuid}
    |
    v
TeamTalkController.getTeamTalk()
    |
    v
TeamTalkService.getTeamTalk()
    |
    +-> getViewSummary()
    |       |
    |       +-> findByTeamTalkUuidOrderByViewedAtDesc()
    |       +-> Calculate total views, unique viewers
    |       +-> Format recent views (last 20)
    |
    v
Return TeamTalkResponse with viewSummary
    |
    v
Frontend displays view stats (coaches only)
```

---

## Performance Considerations

### Database Indexing
- **idx_team_talk:** Optimizes `findByTeamTalkUuid` queries (most common)
- **idx_username:** Optimizes user-specific queries (less common)
- **idx_team_talk_username:** Optimizes combined queries

### Query Optimization
- Use `countBy` methods instead of `findBy().size` for better performance
- Limit recent views to 20 to avoid large payloads
- Consider pagination if view counts grow very large (1000+ views per talk)

### Potential Future Enhancements
- **Caching:** Cache view summaries with TTL (5-10 minutes) to reduce DB load
- **Aggregation Table:** Create daily/weekly aggregated view counts for analytics
- **Lazy Loading:** Only load view summaries when coach explicitly requests them
- **View Deduplication:** Option to track only unique views (requires business decision)

---

## Security Considerations

### Authentication Required
- View tracking endpoint must validate authenticated user
- Username/displayName should come from authenticated session, not client request (prevents spoofing)
- Consider adding `@PreAuthorize("hasRole('ROLE_USER')")` to controller method

### Rate Limiting
- Consider rate limiting view tracking endpoint to prevent abuse
- Limit: ~10 views per user per team talk per minute (allows page refreshes, prevents spam)

### Data Privacy
- View data only visible to coaches (enforced on frontend)
- Consider: Should coaches see other coaches' views? (Currently yes - all views tracked)
- No sensitive data stored (just username, display name, timestamp)

---

## Alternative Approaches Considered

### 1. Unique Views Only
**Rejected:** Requirements specify tracking every view, not just unique views.

### 2. Foreign Key Constraints
**Rejected:** Requirements specify no foreign key on username (soft reference).

### 3. Separate Unique Viewer Count Column
**Rejected:** Can calculate from data; avoids update complexity and potential inconsistency.

### 4. View Tracking in Frontend Only (Analytics)
**Rejected:** Backend tracking provides more reliable data and enables server-side analytics.

### 5. Including Device ID
**Rejected:** Not required for current use case; can add later if needed for device-level analytics.

---

## Example API Responses

### GET /api/v1/team-talks/{uuid}

**Response with View Summary:**

```json
{
  "teamTalk": {
    "uuid": "abc-123-def-456",
    "author": "coach_mike",
    "season": "xc",
    "year": "2024",
    "title": "Pre-Race Strategy",
    "content": "# Race Day Tips\n\n...",
    "team": "UA",
    "createdAt": "2024-10-01T10:00:00Z",
    "updatedAt": "2024-10-01T10:00:00Z"
  },
  "reactions": [
    {
      "emoji": "🔥",
      "count": 12,
      "usernames": ["jdoe", "asmith", "..."]
    }
  ],
  "comments": [...],
  "totalCommentCount": 8,
  "viewSummary": {
    "totalViews": 47,
    "uniqueViewers": 23,
    "recentViews": [
      {
        "username": "jdoe",
        "displayName": "John Doe",
        "viewedAt": "2024-10-15T14:32:10Z"
      },
      {
        "username": "asmith",
        "displayName": "Alice Smith",
        "viewedAt": "2024-10-15T14:30:05Z"
      }
      // ... up to 20 recent views
    ]
  }
}
```

---

## Testing Scenarios

### Manual Testing Script

```bash
# 1. Track a view
curl -X POST http://localhost:8080/api/v1/team-talks/views/track \
  -H "Content-Type: application/json" \
  -d '{
    "teamTalkUuid": "test-uuid-123",
    "username": "testuser1",
    "displayName": "Test User One"
  }'

# 2. Track another view (same user)
curl -X POST http://localhost:8080/api/v1/team-talks/views/track \
  -H "Content-Type: application/json" \
  -d '{
    "teamTalkUuid": "test-uuid-123",
    "username": "testuser1",
    "displayName": "Test User One"
  }'

# 3. Track view from different user
curl -X POST http://localhost:8080/api/v1/team-talks/views/track \
  -H "Content-Type: application/json" \
  -d '{
    "teamTalkUuid": "test-uuid-123",
    "username": "testuser2",
    "displayName": "Test User Two"
  }'

# 4. Get team talk and verify view counts
curl http://localhost:8080/api/v1/team-talks/test-uuid-123

# Expected: totalViews = 3, uniqueViewers = 2
```

### Expected Results
- **Total Views:** 3 (counts all view events)
- **Unique Viewers:** 2 (distinct usernames: testuser1, testuser2)
- **Recent Views:** List of 3 entries, ordered by viewed_at DESC

---

## Summary

This feature provides comprehensive view tracking for team talks with:

✅ **Every view tracked** (not just unique)
✅ **Simple API endpoint** for frontend integration
✅ **Username-based tracking** (no foreign key constraints)
✅ **Coach-only visibility** on frontend
✅ **No gamification** (no points awarded)
✅ **Efficient queries** with proper indexing
✅ **Consistent data model** with existing team talk features
✅ **Extensible design** for future analytics enhancements

The implementation follows existing patterns in the codebase (similar to reactions and comments) and integrates seamlessly with the current `TeamTalkResponse` structure.
