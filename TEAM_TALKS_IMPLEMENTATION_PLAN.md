# Team Talks Feature - Implementation Plan

## Overview
Implement a discussion feature where coaches can create markdown-formatted "team talks" that athletes can comment on (with nested/threaded replies) and react to with emojis. Athletes earn 3 pride points for commenting.

## Requirements Summary
- **Team Talks**: Markdown content with author, season (xc/track), year, title
- **Storage**: Database TEXT column (not file system)
- **Reactions**: Multiple emoji reactions (Slack/Discord style)
- **Comments**: Nested/threaded (new pattern for this codebase)
- **Permissions**: Only coaches can create team talks
- **Points**: Athletes earn 3 points per comment

---

## Database Schema

### 1. team_talks table
```sql
CREATE TABLE team_talks (
    id INT PRIMARY KEY AUTO_INCREMENT,
    uuid VARCHAR(255) UNIQUE NOT NULL,
    author VARCHAR(100) NOT NULL COMMENT 'Coach username/ID who created the talk',
    season VARCHAR(20) NOT NULL COMMENT 'xc or track',
    year VARCHAR(10) NOT NULL COMMENT 'e.g., 2024, 2025',
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL COMMENT 'Markdown text content',
    team VARCHAR(20) NOT NULL DEFAULT 'UA',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_season_year (season, year, created_at DESC),
    INDEX idx_uuid (uuid),
    INDEX idx_team_season (team, season, year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Coach team talks stored as markdown';
```

### 2. team_talk_reactions table
```sql
CREATE TABLE team_talk_reactions (
    id INT PRIMARY KEY AUTO_INCREMENT,
    team_talk_uuid VARCHAR(255) NOT NULL,
    runner_id INT NOT NULL,
    emoji VARCHAR(20) NOT NULL COMMENT 'Unicode emoji character(s)',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY unique_reaction (team_talk_uuid, runner_id, emoji),
    INDEX idx_team_talk (team_talk_uuid),
    INDEX idx_runner (runner_id),
    FOREIGN KEY (runner_id) REFERENCES runners(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Emoji reactions on team talks';
```

### 3. team_talk_comments table (threaded)
```sql
CREATE TABLE team_talk_comments (
    id INT PRIMARY KEY AUTO_INCREMENT,
    uuid VARCHAR(255) UNIQUE NOT NULL,
    team_talk_uuid VARCHAR(255) NOT NULL,
    parent_comment_uuid VARCHAR(255) NULL COMMENT 'NULL for top-level, UUID for replies',
    runner_id INT NOT NULL COMMENT 'Athlete who commented',
    runner_name VARCHAR(255) NOT NULL COMMENT 'Cached runner name for display',
    message TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_team_talk (team_talk_uuid, created_at),
    INDEX idx_parent (parent_comment_uuid),
    INDEX idx_runner (runner_id),
    FOREIGN KEY (runner_id) REFERENCES runners(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Threaded comments on team talks';
```

### 4. Points configuration
```sql
INSERT INTO points_configuration (config_key, config_value, description)
VALUES ('EARN_TEAM_TALK_COMMENT', 3, 'Points awarded for commenting on a team talk');
```

---

## File Structure

### New Files to Create

#### Models (src/main/kotlin/com/terkula/uaxctf/statisitcs/model/)

**TeamTalk.kt**
```kotlin
package com.terkula.uaxctf.statisitcs.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "team_talks", schema = "uaxc")
@JsonInclude(value = JsonInclude.Include.NON_NULL)
class TeamTalk(
    @Column(name = "uuid", unique = true, nullable = false)
    var uuid: String,

    @Column(name = "author", nullable = false, length = 100)
    var author: String,

    @Column(name = "season", nullable = false, length = 20)
    var season: String,

    @Column(name = "year", nullable = false, length = 10)
    var year: String,

    @Column(name = "title", nullable = false)
    var title: String,

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    var content: String,

    @Column(name = "team", nullable = false, length = 20)
    var team: String = "UA",

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Timestamp = Timestamp(System.currentTimeMillis()),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Timestamp = Timestamp(System.currentTimeMillis())
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    val id: Int = 0

    @PrePersist
    protected fun onCreate() {
        createdAt = Timestamp(System.currentTimeMillis())
        updatedAt = Timestamp(System.currentTimeMillis())
    }

    @PreUpdate
    protected fun onUpdate() {
        updatedAt = Timestamp(System.currentTimeMillis())
    }
}
```

**TeamTalkReaction.kt**
```kotlin
package com.terkula.uaxctf.statisitcs.model

import com.fasterxml.jackson.annotation.JsonIgnore
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(
    name = "team_talk_reactions",
    schema = "uaxc",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["team_talk_uuid", "runner_id", "emoji"])
    ]
)
class TeamTalkReaction(
    @Column(name = "team_talk_uuid", nullable = false)
    var teamTalkUuid: String,

    @Column(name = "runner_id", nullable = false)
    var runnerId: Int,

    @Column(name = "emoji", nullable = false, length = 20)
    var emoji: String,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Timestamp = Timestamp(System.currentTimeMillis())
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    val id: Int = 0

    @PrePersist
    protected fun onCreate() {
        createdAt = Timestamp(System.currentTimeMillis())
    }
}
```

**TeamTalkComment.kt**
```kotlin
package com.terkula.uaxctf.statisitcs.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "team_talk_comments", schema = "uaxc")
@JsonInclude(value = JsonInclude.Include.NON_NULL)
class TeamTalkComment(
    @Column(name = "uuid", unique = true, nullable = false)
    var uuid: String,

    @Column(name = "team_talk_uuid", nullable = false)
    var teamTalkUuid: String,

    @Column(name = "parent_comment_uuid")
    var parentCommentUuid: String? = null,

    @Column(name = "runner_id", nullable = false)
    var runnerId: Int,

    @Column(name = "runner_name", nullable = false)
    var runnerName: String,

    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    var message: String,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Timestamp = Timestamp(System.currentTimeMillis()),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Timestamp = Timestamp(System.currentTimeMillis())
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    val id: Int = 0

    @PrePersist
    protected fun onCreate() {
        createdAt = Timestamp(System.currentTimeMillis())
        updatedAt = Timestamp(System.currentTimeMillis())
    }

    @PreUpdate
    protected fun onUpdate() {
        updatedAt = Timestamp(System.currentTimeMillis())
    }
}
```

#### Repositories (src/main/kotlin/com/terkula/uaxctf/statistics/repository/)

**TeamTalkRepository.kt**
```kotlin
package com.terkula.uaxctf.statistics.repository

import com.terkula.uaxctf.statisitcs.model.TeamTalk
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface TeamTalkRepository : CrudRepository<TeamTalk, Int> {
    fun findByUuid(uuid: String): TeamTalk?
    fun findBySeasonAndYearOrderByCreatedAtDesc(season: String, year: String): List<TeamTalk>
    fun findBySeasonAndYearAndTeamOrderByCreatedAtDesc(season: String, year: String, team: String): List<TeamTalk>
    fun findAllByOrderByCreatedAtDesc(): List<TeamTalk>
}
```

**TeamTalkReactionRepository.kt**
```kotlin
package com.terkula.uaxctf.statistics.repository

import com.terkula.uaxctf.statisitcs.model.TeamTalkReaction
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface TeamTalkReactionRepository : CrudRepository<TeamTalkReaction, Int> {
    fun findByTeamTalkUuid(teamTalkUuid: String): List<TeamTalkReaction>
    fun findByTeamTalkUuidAndRunnerId(teamTalkUuid: String, runnerId: Int): List<TeamTalkReaction>
    fun findByTeamTalkUuidAndRunnerIdAndEmoji(teamTalkUuid: String, runnerId: Int, emoji: String): TeamTalkReaction?
    fun deleteByTeamTalkUuidAndRunnerIdAndEmoji(teamTalkUuid: String, runnerId: Int, emoji: String): Int
}
```

**TeamTalkCommentRepository.kt**
```kotlin
package com.terkula.uaxctf.statistics.repository

import com.terkula.uaxctf.statisitcs.model.TeamTalkComment
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface TeamTalkCommentRepository : CrudRepository<TeamTalkComment, Int> {
    fun findByUuid(uuid: String): TeamTalkComment?
    fun findByTeamTalkUuidOrderByCreatedAtAsc(teamTalkUuid: String): List<TeamTalkComment>
    fun findByTeamTalkUuidAndParentCommentUuidIsNullOrderByCreatedAtAsc(teamTalkUuid: String): List<TeamTalkComment>
    fun findByParentCommentUuidOrderByCreatedAtAsc(parentCommentUuid: String): List<TeamTalkComment>
}
```

#### Request/Response DTOs

**src/main/kotlin/com/terkula/uaxctf/statistics/request/TeamTalkRequests.kt**
```kotlin
package com.terkula.uaxctf.statistics.request

import com.fasterxml.jackson.annotation.JsonFormat
import java.sql.Timestamp

data class CreateTeamTalkRequest(
    val uuid: String,
    val author: String,
    val season: String,
    val year: String,
    val title: String,
    val content: String,
    val team: String = "UA"
)

data class UpdateTeamTalkRequest(
    val uuid: String,
    val title: String,
    val content: String
)

data class CreateTeamTalkCommentRequest(
    val uuid: String,
    val teamTalkUuid: String,
    val parentCommentUuid: String? = null,
    val runnerId: Int,
    val message: String,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
    val timestamp: Timestamp,
    val season: String,
    val year: String
)

data class AddTeamTalkReactionRequest(
    val teamTalkUuid: String,
    val runnerId: Int,
    val emoji: String
)

data class RemoveTeamTalkReactionRequest(
    val teamTalkUuid: String,
    val runnerId: Int,
    val emoji: String
)
```

**src/main/kotlin/com/terkula/uaxctf/statistics/response/TeamTalkResponses.kt**
```kotlin
package com.terkula.uaxctf.statistics.response

import com.terkula.uaxctf.statisitcs.model.TeamTalk
import com.terkula.uaxctf.statisitcs.model.TeamTalkComment
import java.sql.Timestamp

data class ReactionSummary(
    val emoji: String,
    val count: Int,
    val userIds: List<Int>
)

data class NestedComment(
    val uuid: String,
    val teamTalkUuid: String,
    val runnerId: Int,
    val runnerName: String,
    val message: String,
    val createdAt: Timestamp,
    val updatedAt: Timestamp,
    val replies: List<NestedComment> = emptyList()
)

data class TeamTalkResponse(
    val teamTalk: TeamTalk,
    val reactions: List<ReactionSummary>,
    val comments: List<NestedComment>,
    val totalCommentCount: Int
)

data class TeamTalkListResponse(
    val teamTalks: List<TeamTalkResponse>
)

data class CommentCreatedResponse(
    val comment: TeamTalkComment,
    val pointsEarned: Int,
    val newPointBalance: Int
)
```

#### Service Layer

**src/main/kotlin/com/terkula/uaxctf/statistics/service/TeamTalkService.kt**

Key methods:
- `createTeamTalk()` - Create new team talk
- `updateTeamTalk()` - Update existing team talk
- `getTeamTalk(uuid)` - Get single team talk with reactions and nested comments
- `getTeamTalksBySeason()` - Get all team talks for a season/year
- `addReaction()` / `removeReaction()` - Manage emoji reactions
- `createComment()` - Create comment with automatic points award
- `getNestedComments()` - **Critical method** - Builds nested comment tree efficiently

**Nested Comment Algorithm (Avoids N+1 Queries):**
```kotlin
private fun getNestedComments(teamTalkUuid: String): List<NestedComment> {
    // 1. Fetch ALL comments in single query
    val allComments = teamTalkCommentRepository.findByTeamTalkUuidOrderByCreatedAtAsc(teamTalkUuid)

    // 2. Group by parent UUID in memory
    val commentsByParent = allComments.groupBy { it.parentCommentUuid }

    // 3. Get top-level comments (parent = null)
    val topLevel = commentsByParent[null] ?: emptyList()

    // 4. Build recursive tree
    return topLevel.map { buildNestedComment(it, commentsByParent) }
}

private fun buildNestedComment(
    comment: TeamTalkComment,
    commentsByParent: Map<String?, List<TeamTalkComment>>
): NestedComment {
    val replies = commentsByParent[comment.uuid] ?: emptyList()

    return NestedComment(
        uuid = comment.uuid,
        teamTalkUuid = comment.teamTalkUuid,
        runnerId = comment.runnerId,
        runnerName = comment.runnerName,
        message = comment.message,
        createdAt = comment.createdAt,
        updatedAt = comment.updatedAt,
        replies = replies.map { buildNestedComment(it, commentsByParent) }
    )
}
```

**Points Integration:**
```kotlin
@Transactional
fun createComment(request: CreateTeamTalkCommentRequest): CommentCreatedResponse {
    val runner = runnerRepository.findById(request.runnerId)
        .orElseThrow { RuntimeException("Runner not found") }

    val comment = TeamTalkComment(
        uuid = request.uuid,
        teamTalkUuid = request.teamTalkUuid,
        parentCommentUuid = request.parentCommentUuid,
        runnerId = request.runnerId,
        runnerName = runner.name,
        message = request.message
    )

    teamTalkCommentRepository.save(comment)

    // Award points
    var pointsEarned = 0
    var newBalance = runner.points

    try {
        val pointsResponse = pointsService.earnPoints(EarnPointsRequest(
            runnerId = request.runnerId,
            activityType = "TEAM_TALK_COMMENT",
            activityUuid = request.uuid,
            season = request.season,
            year = request.year,
            description = "Commented on team talk"
        ))
        pointsEarned = pointsResponse.pointsEarned
        newBalance = pointsResponse.newBalance
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return CommentCreatedResponse(comment, pointsEarned, newBalance)
}
```

#### Controller

**src/main/kotlin/com/terkula/uaxctf/statistics/controller/TeamTalkController.kt**

REST endpoints:
```kotlin
@RestController
@RequestMapping("/api/v1/team-talks")
class TeamTalkController(val teamTalkService: TeamTalkService) {

    @PostMapping("")
    fun createTeamTalk(@RequestBody request: CreateTeamTalkRequest): TeamTalkResponse

    @PutMapping("")
    fun updateTeamTalk(@RequestBody request: UpdateTeamTalkRequest): TeamTalkResponse

    @GetMapping("/{uuid}")
    fun getTeamTalk(@PathVariable uuid: String): TeamTalkResponse

    @GetMapping("/season")
    fun getTeamTalksBySeason(
        @RequestParam season: String,
        @RequestParam year: String,
        @RequestParam(defaultValue = "UA") team: String
    ): TeamTalkListResponse

    @GetMapping("/all")
    fun getAllTeamTalks(): TeamTalkListResponse

    @PostMapping("/reactions/add")
    fun addReaction(@RequestBody request: AddTeamTalkReactionRequest): List<ReactionSummary>

    @PostMapping("/reactions/remove")
    fun removeReaction(@RequestBody request: RemoveTeamTalkReactionRequest): List<ReactionSummary>

    @PostMapping("/comments")
    fun createComment(@RequestBody request: CreateTeamTalkCommentRequest): CommentCreatedResponse
}
```

#### Database Migrations

**db-migrations/add_team_talks.sql**
- Contains all 3 table creation statements
- Contains points_configuration insert

### Files to Modify

**src/main/kotlin/com/terkula/uaxctf/statisitcs/model/PointTransaction.kt**
```kotlin
enum class ActivityType {
    TRAINING_RUN,
    WORKOUT,
    CROSS_TRAINING,
    MEET_LOG,
    PRE_MEET_LOG,
    TEAM_TALK_COMMENT  // ADD THIS
}
```

---

## API Endpoints Summary

```
POST   /api/v1/team-talks                    - Create team talk (coaches only)
PUT    /api/v1/team-talks                    - Update team talk (coaches only)
GET    /api/v1/team-talks/{uuid}             - Get single team talk with all data
GET    /api/v1/team-talks/season             - Get team talks by season/year
       ?season=xc&year=2024&team=UA
GET    /api/v1/team-talks/all                - Get all team talks

POST   /api/v1/team-talks/reactions/add      - Add emoji reaction
POST   /api/v1/team-talks/reactions/remove   - Remove emoji reaction

POST   /api/v1/team-talks/comments           - Create comment (awards 3 points)
```

---

## Implementation Sequence

1. **Database Setup**
   - Create `db-migrations/add_team_talks.sql` with all 3 tables + points config
   - Run migration on development database
   - Verify tables with `DESCRIBE team_talks`, etc.

2. **Core Models & Repositories**
   - Create TeamTalk, TeamTalkReaction, TeamTalkComment entities
   - Create repository interfaces
   - Update PointTransaction.ActivityType enum

3. **DTOs**
   - Create TeamTalkRequests.kt with all request classes
   - Create TeamTalkResponses.kt with response classes including NestedComment

4. **Service Layer**
   - Implement TeamTalkService
   - Focus on nested comment algorithm (critical for performance)
   - Integrate with PointsService for comment awards
   - Implement reaction aggregation

5. **Controller**
   - Implement TeamTalkController with all endpoints
   - Add Swagger documentation with @ApiOperation annotations

6. **Testing**
   - Test nested comment structure with 3+ levels
   - Test points awarded correctly (3 points per comment)
   - Test reaction add/remove idempotency
   - Test season/year filtering

7. **iOS Integration Documentation**
   - Provide example API calls and responses
   - Document markdown rendering approach

---

## Critical Implementation Notes

### Nested Comments (NEW Pattern)
- Existing TrainingComment and JournalComment are **flat only**
- TeamTalkComment introduces **parent-child relationships**
- Use single-query + in-memory tree building to avoid N+1 queries
- `parentCommentUuid = null` indicates top-level comment
- Recursive algorithm builds nested structure efficiently

### Emoji Reactions
- Store emoji as VARCHAR(20) with utf8mb4 charset
- iOS sends Unicode emoji directly: `{"emoji": "👍"}`
- Backend stores raw Unicode string
- Aggregate by emoji type for display (count + user IDs)
- Unique constraint prevents duplicate reactions

### Markdown Handling
- **Backend**: Store raw markdown, no processing or validation
- **iOS**: Use markdown rendering library (e.g., Down, MarkdownView)
- Support: headers, bold, italic, lists, links, basic formatting

### Permission Enforcement
- **MVP**: Trust iOS to enforce "coaches only create" rule
- **Future**: Add server-side role checking if auth system supports it

### Points Integration
- Uses existing PointsService pattern
- Activity type: `TEAM_TALK_COMMENT`
- Activity UUID: Comment UUID (for idempotency)
- Award 3 points per comment (configured in database)
- Points failure doesn't fail comment creation (graceful degradation)

---

## Example API Response

**GET /api/v1/team-talks/{uuid}**

```json
{
  "teamTalk": {
    "uuid": "team-talk-123",
    "author": "Coach Smith",
    "season": "xc",
    "year": "2024",
    "title": "Great Work This Weekend!",
    "content": "# Excellent Races!\n\nYou all ran **amazing** races...",
    "team": "UA",
    "createdAt": "2024-11-15T10:00:00",
    "updatedAt": "2024-11-15T10:00:00"
  },
  "reactions": [
    {
      "emoji": "👍",
      "count": 8,
      "userIds": [1, 2, 3, 4, 5, 6, 7, 8]
    },
    {
      "emoji": "❤️",
      "count": 5,
      "userIds": [1, 3, 5, 9, 11]
    },
    {
      "emoji": "🔥",
      "count": 3,
      "userIds": [2, 4, 6]
    }
  ],
  "comments": [
    {
      "uuid": "comment-1",
      "teamTalkUuid": "team-talk-123",
      "runnerId": 89,
      "runnerName": "John Doe",
      "message": "Thanks coach! That was a tough race.",
      "createdAt": "2024-11-15T11:00:00",
      "updatedAt": "2024-11-15T11:00:00",
      "replies": [
        {
          "uuid": "comment-2",
          "teamTalkUuid": "team-talk-123",
          "runnerId": 90,
          "runnerName": "Jane Smith",
          "message": "I agree! The hills were brutal.",
          "createdAt": "2024-11-15T11:30:00",
          "updatedAt": "2024-11-15T11:30:00",
          "replies": []
        }
      ]
    }
  ],
  "totalCommentCount": 2
}
```

---

## iOS Integration Guide

### Displaying Markdown
Use a Swift markdown library:
```swift
import Down

let markdown = teamTalk.content
let down = Down(markdownString: markdown)
let attributedString = try? down.toAttributedString()
textView.attributedText = attributedString
```

### Displaying Nested Comments
Flatten the nested structure for UITableView:
```swift
struct FlatComment {
    let comment: NestedComment
    let depth: Int
}

func flattenComments(_ comments: [NestedComment], depth: Int = 0) -> [FlatComment] {
    var result: [FlatComment] = []
    for comment in comments {
        result.append(FlatComment(comment: comment, depth: depth))
        result.append(contentsOf: flattenComments(comment.replies, depth: depth + 1))
    }
    return result
}
```

Then indent cells based on depth level.

### Creating a Reply
```swift
// When user taps "Reply" on a comment
let request = CreateTeamTalkCommentRequest(
    uuid: UUID().uuidString,
    teamTalkUuid: teamTalk.uuid,
    parentCommentUuid: parentComment.uuid,  // Link to parent
    runnerId: currentUser.id,
    message: userInput,
    timestamp: Date(),
    season: teamTalk.season,
    year: teamTalk.year
)
```

### Adding Reactions
```swift
// When user taps emoji picker
let request = AddTeamTalkReactionRequest(
    teamTalkUuid: teamTalk.uuid,
    runnerId: currentUser.id,
    emoji: "👍"
)

// Response includes updated reaction summary
// Update UI to show new count
```

---

## Success Criteria

- ✅ Coaches can create/edit team talks with markdown content
- ✅ Athletes can view team talks with properly rendered markdown (iOS)
- ✅ Athletes can add nested comments and earn 3 points per comment
- ✅ Athletes can add/remove emoji reactions
- ✅ Comments display in threaded structure (replies nested under parents)
- ✅ Reactions aggregated by emoji type with counts and user lists
- ✅ No N+1 query issues when loading team talk with many comments
- ✅ Single API call returns complete team talk with reactions and nested comments
- ✅ Points integration works correctly with idempotency