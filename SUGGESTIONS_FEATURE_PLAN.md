# Runner Suggestions Feature - Implementation Plan

## Overview
A feature allowing runners to submit suggestions for app improvements. Other runners can thumbs up suggestions, and coaches can manage suggestion status and delete them.

## Requirements
- Runners can submit suggestions with title, description, and category
- Other runners can give thumbs up to suggestions (one per runner per suggestion)
- Suggestions are visible to all runners
- Coaches can change suggestion status (under review, under consideration, in progress, complete, rejected)
- Anyone can leave comments on suggestions
- Coaches can hard delete suggestions
- Track creation date and status change date

## Database Schema

### Table: `suggestions`
```sql
CREATE TABLE uaxc.suggestions (
    id INT PRIMARY KEY AUTO_INCREMENT,
    uuid VARCHAR(255) UNIQUE NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(100) NOT NULL,
    runner_id INT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'under_review',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status_changed_at TIMESTAMP NULL,
    team VARCHAR(50) NOT NULL,
    season VARCHAR(10),
    INDEX idx_runner_id (runner_id),
    INDEX idx_status (status),
    INDEX idx_category (category),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (runner_id) REFERENCES runners(id)
);
```

**Status Values:**
- `under_review` - Initial status, coaches reviewing
- `under_consideration` - Being considered for implementation
- `in_progress` - Currently being worked on
- `complete` - Implemented and deployed
- `rejected` - Not moving forward

**Category Values (suggested):**
- `feature_request` - New features
- `bug_report` - Issues/bugs
- `ui_ux` - User interface/experience improvements
- `performance` - Speed/performance issues
- `other` - Miscellaneous suggestions

### Table: `suggestion_thumbs_up`
```sql
CREATE TABLE uaxc.suggestion_thumbs_up (
    id INT PRIMARY KEY AUTO_INCREMENT,
    suggestion_uuid VARCHAR(255) NOT NULL,
    runner_id INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_thumbs_up (suggestion_uuid, runner_id),
    INDEX idx_suggestion_uuid (suggestion_uuid),
    FOREIGN KEY (runner_id) REFERENCES runners(id)
);
```

### Table: `suggestion_comments`
```sql
CREATE TABLE uaxc.suggestion_comments (
    id INT PRIMARY KEY AUTO_INCREMENT,
    uuid VARCHAR(255) UNIQUE NOT NULL,
    suggestion_uuid VARCHAR(255) NOT NULL,
    username VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_suggestion_uuid (suggestion_uuid),
    INDEX idx_created_at (created_at)
);
```

## Model Classes

### Location: `src/main/kotlin/com/terkula/uaxctf/statistics/model/`

### 1. Suggestion.kt
```kotlin
package com.terkula.uaxctf.statisitcs.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "suggestions", schema = "uaxc")
@JsonInclude(value = JsonInclude.Include.NON_NULL)
class Suggestion(
    @Column(name = "uuid", unique = true, nullable = false)
    var uuid: String,

    @Column(name = "title", nullable = false)
    var title: String,

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    var description: String,

    @Column(name = "category", nullable = false)
    var category: String,

    @Column(name = "runner_id", nullable = false)
    var runnerId: Int,

    @Column(name = "status", nullable = false)
    var status: String = "under_review",

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Timestamp = Timestamp(System.currentTimeMillis()),

    @Column(name = "status_changed_at")
    var statusChangedAt: Timestamp? = null,

    @Column(name = "team", nullable = false)
    var team: String,

    @Column(name = "season")
    var season: String? = null
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

### 2. SuggestionThumbsUp.kt
```kotlin
package com.terkula.uaxctf.statisitcs.model

import com.fasterxml.jackson.annotation.JsonIgnore
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(
    name = "suggestion_thumbs_up",
    schema = "uaxc",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["suggestion_uuid", "runner_id"])
    ]
)
class SuggestionThumbsUp(
    @Column(name = "suggestion_uuid", nullable = false)
    var suggestionUuid: String,

    @Column(name = "runner_id", nullable = false)
    var runnerId: Int,

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

### 3. SuggestionComment.kt
```kotlin
package com.terkula.uaxctf.statisitcs.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "suggestion_comments", schema = "uaxc")
@JsonInclude(value = JsonInclude.Include.NON_NULL)
class SuggestionComment(
    @Column(name = "uuid", unique = true, nullable = false)
    var uuid: String,

    @Column(name = "suggestion_uuid", nullable = false)
    var suggestionUuid: String,

    @Column(name = "username", nullable = false)
    var username: String,

    @Column(name = "display_name", nullable = false)
    var displayName: String,

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

## Repository Interfaces

### Location: `src/main/kotlin/com/terkula/uaxctf/statistics/repository/`

### 1. SuggestionRepository.kt
```kotlin
package com.terkula.uaxctf.statistics.repository

import com.terkula.uaxctf.statisitcs.model.Suggestion
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface SuggestionRepository : CrudRepository<Suggestion, Int> {
    fun findByUuid(uuid: String): Suggestion?
    fun findByRunnerId(runnerId: Int): List<Suggestion>
    fun findByStatus(status: String): List<Suggestion>
    fun findByCategory(category: String): List<Suggestion>
    fun findByTeam(team: String): List<Suggestion>
    fun findByTeamOrderByCreatedAtDesc(team: String): List<Suggestion>
    fun deleteByUuid(uuid: String)
}
```

### 2. SuggestionThumbsUpRepository.kt
```kotlin
package com.terkula.uaxctf.statistics.repository

import com.terkula.uaxctf.statisitcs.model.SuggestionThumbsUp
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface SuggestionThumbsUpRepository : CrudRepository<SuggestionThumbsUp, Int> {
    fun findBySuggestionUuid(suggestionUuid: String): List<SuggestionThumbsUp>
    fun findBySuggestionUuidAndRunnerId(suggestionUuid: String, runnerId: Int): SuggestionThumbsUp?
    fun countBySuggestionUuid(suggestionUuid: String): Long
    fun deleteBySuggestionUuidAndRunnerId(suggestionUuid: String, runnerId: Int)
    fun deleteBySuggestionUuid(suggestionUuid: String)
}
```

### 3. SuggestionCommentRepository.kt
```kotlin
package com.terkula.uaxctf.statistics.repository

import com.terkula.uaxctf.statisitcs.model.SuggestionComment
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface SuggestionCommentRepository : CrudRepository<SuggestionComment, Int> {
    fun findByUuid(uuid: String): SuggestionComment?
    fun findBySuggestionUuidOrderByCreatedAtAsc(suggestionUuid: String): List<SuggestionComment>
    fun deleteBySuggestionUuid(suggestionUuid: String)
}
```

## Request/Response DTOs

### Location: `src/main/kotlin/com/terkula/uaxctf/statistics/request/SuggestionRequests.kt`

```kotlin
package com.terkula.uaxctf.statistics.request

data class CreateSuggestionRequest(
    val uuid: String,
    val title: String,
    val description: String,
    val category: String,
    val runnerId: Int,
    val team: String,
    val season: String?
)

data class UpdateSuggestionStatusRequest(
    val suggestionUuid: String,
    val status: String
)

data class ToggleThumbsUpRequest(
    val suggestionUuid: String,
    val runnerId: Int
)

data class CreateSuggestionCommentRequest(
    val uuid: String,
    val suggestionUuid: String,
    val username: String,
    val displayName: String,
    val message: String
)
```

### Location: `src/main/kotlin/com/terkula/uaxctf/statistics/response/SuggestionResponses.kt`

```kotlin
package com.terkula.uaxctf.statistics.response

import com.fasterxml.jackson.annotation.JsonInclude
import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.statisitcs.model.Suggestion
import com.terkula.uaxctf.statisitcs.model.SuggestionComment
import java.sql.Timestamp

@JsonInclude(JsonInclude.Include.NON_NULL)
data class SuggestionDTO(
    val uuid: String,
    val title: String,
    val description: String,
    val category: String,
    val runner: Runner,
    val status: String,
    val createdAt: Timestamp,
    val statusChangedAt: Timestamp?,
    val thumbsUpCount: Long,
    val hasThumbsUp: Boolean? = null,  // If checking for specific runner
    val commentCount: Int
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class SuggestionDetailResponse(
    val suggestion: SuggestionDTO,
    val comments: List<SuggestionComment>
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class SuggestionsListResponse(
    val suggestions: List<SuggestionDTO>
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ThumbsUpResponse(
    val suggestionUuid: String,
    val thumbsUpCount: Long,
    val hasThumbsUp: Boolean
)
```

## Service Layer

### Location: `src/main/kotlin/com/terkula/uaxctf/statistics/service/SuggestionService.kt`

```kotlin
package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.statistics.repository.SuggestionCommentRepository
import com.terkula.uaxctf.statistics.repository.SuggestionRepository
import com.terkula.uaxctf.statistics.repository.SuggestionThumbsUpRepository
import com.terkula.uaxctf.statistics.request.CreateSuggestionCommentRequest
import com.terkula.uaxctf.statistics.request.CreateSuggestionRequest
import com.terkula.uaxctf.statistics.request.ToggleThumbsUpRequest
import com.terkula.uaxctf.statistics.request.UpdateSuggestionStatusRequest
import com.terkula.uaxctf.statistics.response.SuggestionDTO
import com.terkula.uaxctf.statistics.response.SuggestionDetailResponse
import com.terkula.uaxctf.statistics.response.SuggestionsListResponse
import com.terkula.uaxctf.statistics.response.ThumbsUpResponse
import com.terkula.uaxctf.statisitcs.model.Suggestion
import com.terkula.uaxctf.statisitcs.model.SuggestionComment
import com.terkula.uaxctf.statisitcs.model.SuggestionThumbsUp
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp

@Service
class SuggestionService(
    val suggestionRepository: SuggestionRepository,
    val suggestionThumbsUpRepository: SuggestionThumbsUpRepository,
    val suggestionCommentRepository: SuggestionCommentRepository,
    val runnerRepository: RunnerRepository
) {

    // Create a new suggestion
    @Transactional
    fun createSuggestion(request: CreateSuggestionRequest): SuggestionDTO {
        val runner = runnerRepository.findById(request.runnerId)
            .orElseThrow { IllegalArgumentException("Runner not found: ${request.runnerId}") }

        val suggestion = Suggestion(
            uuid = request.uuid,
            title = request.title,
            description = request.description,
            category = request.category,
            runnerId = request.runnerId,
            status = "under_review",
            team = request.team,
            season = request.season
        )

        val saved = suggestionRepository.save(suggestion)
        return toDTO(saved, runner, 0L, false, 0)
    }

    // Get all suggestions for a team
    fun getSuggestions(team: String, runnerId: Int? = null): SuggestionsListResponse {
        val suggestions = suggestionRepository.findByTeamOrderByCreatedAtDesc(team)
        val dtos = suggestions.map { suggestion ->
            val runner = runnerRepository.findById(suggestion.runnerId).orElse(null)
            val thumbsUpCount = suggestionThumbsUpRepository.countBySuggestionUuid(suggestion.uuid)
            val hasThumbsUp = runnerId?.let {
                suggestionThumbsUpRepository.findBySuggestionUuidAndRunnerId(suggestion.uuid, it) != null
            }
            val commentCount = suggestionCommentRepository.findBySuggestionUuidOrderByCreatedAtAsc(suggestion.uuid).size
            toDTO(suggestion, runner, thumbsUpCount, hasThumbsUp, commentCount)
        }
        return SuggestionsListResponse(dtos)
    }

    // Get single suggestion with comments
    fun getSuggestionDetail(uuid: String, runnerId: Int? = null): SuggestionDetailResponse {
        val suggestion = suggestionRepository.findByUuid(uuid)
            ?: throw IllegalArgumentException("Suggestion not found: $uuid")

        val runner = runnerRepository.findById(suggestion.runnerId).orElse(null)
        val thumbsUpCount = suggestionThumbsUpRepository.countBySuggestionUuid(uuid)
        val hasThumbsUp = runnerId?.let {
            suggestionThumbsUpRepository.findBySuggestionUuidAndRunnerId(uuid, it) != null
        }
        val comments = suggestionCommentRepository.findBySuggestionUuidOrderByCreatedAtAsc(uuid)
        val commentCount = comments.size

        return SuggestionDetailResponse(
            suggestion = toDTO(suggestion, runner, thumbsUpCount, hasThumbsUp, commentCount),
            comments = comments
        )
    }

    // Update suggestion status (coaches only - validation should be in controller)
    @Transactional
    fun updateStatus(request: UpdateSuggestionStatusRequest): SuggestionDTO {
        val suggestion = suggestionRepository.findByUuid(request.suggestionUuid)
            ?: throw IllegalArgumentException("Suggestion not found: ${request.suggestionUuid}")

        val validStatuses = listOf("under_review", "under_consideration", "in_progress", "complete", "rejected")
        if (!validStatuses.contains(request.status)) {
            throw IllegalArgumentException("Invalid status: ${request.status}")
        }

        suggestion.status = request.status
        suggestion.statusChangedAt = Timestamp(System.currentTimeMillis())

        val saved = suggestionRepository.save(suggestion)
        val runner = runnerRepository.findById(saved.runnerId).orElse(null)
        val thumbsUpCount = suggestionThumbsUpRepository.countBySuggestionUuid(saved.uuid)
        val commentCount = suggestionCommentRepository.findBySuggestionUuidOrderByCreatedAtAsc(saved.uuid).size

        return toDTO(saved, runner, thumbsUpCount, false, commentCount)
    }

    // Toggle thumbs up for a suggestion
    @Transactional
    fun toggleThumbsUp(request: ToggleThumbsUpRequest): ThumbsUpResponse {
        val existing = suggestionThumbsUpRepository.findBySuggestionUuidAndRunnerId(
            request.suggestionUuid,
            request.runnerId
        )

        if (existing != null) {
            // Remove thumbs up
            suggestionThumbsUpRepository.deleteBySuggestionUuidAndRunnerId(
                request.suggestionUuid,
                request.runnerId
            )
            val count = suggestionThumbsUpRepository.countBySuggestionUuid(request.suggestionUuid)
            return ThumbsUpResponse(request.suggestionUuid, count, false)
        } else {
            // Add thumbs up
            val thumbsUp = SuggestionThumbsUp(
                suggestionUuid = request.suggestionUuid,
                runnerId = request.runnerId
            )
            suggestionThumbsUpRepository.save(thumbsUp)
            val count = suggestionThumbsUpRepository.countBySuggestionUuid(request.suggestionUuid)
            return ThumbsUpResponse(request.suggestionUuid, count, true)
        }
    }

    // Add comment to suggestion
    @Transactional
    fun addComment(request: CreateSuggestionCommentRequest): SuggestionComment {
        val suggestion = suggestionRepository.findByUuid(request.suggestionUuid)
            ?: throw IllegalArgumentException("Suggestion not found: ${request.suggestionUuid}")

        val comment = SuggestionComment(
            uuid = request.uuid,
            suggestionUuid = request.suggestionUuid,
            username = request.username,
            displayName = request.displayName,
            message = request.message
        )

        return suggestionCommentRepository.save(comment)
    }

    // Delete suggestion (coaches only - validation should be in controller)
    @Transactional
    fun deleteSuggestion(uuid: String) {
        // Delete associated thumbs up
        suggestionThumbsUpRepository.deleteBySuggestionUuid(uuid)
        // Delete associated comments
        suggestionCommentRepository.deleteBySuggestionUuid(uuid)
        // Delete suggestion
        suggestionRepository.deleteByUuid(uuid)
    }

    // Helper to convert to DTO
    private fun toDTO(
        suggestion: Suggestion,
        runner: com.terkula.uaxctf.statisitcs.model.Runner?,
        thumbsUpCount: Long,
        hasThumbsUp: Boolean?,
        commentCount: Int
    ): SuggestionDTO {
        return SuggestionDTO(
            uuid = suggestion.uuid,
            title = suggestion.title,
            description = suggestion.description,
            category = suggestion.category,
            runner = runner!!,
            status = suggestion.status,
            createdAt = suggestion.createdAt,
            statusChangedAt = suggestion.statusChangedAt,
            thumbsUpCount = thumbsUpCount,
            hasThumbsUp = hasThumbsUp,
            commentCount = commentCount
        )
    }
}
```

## Controller Endpoints

### Location: `src/main/kotlin/com/terkula/uaxctf/statistics/controller/SuggestionController.kt`

```kotlin
package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statistics.request.CreateSuggestionCommentRequest
import com.terkula.uaxctf.statistics.request.CreateSuggestionRequest
import com.terkula.uaxctf.statistics.request.ToggleThumbsUpRequest
import com.terkula.uaxctf.statistics.request.UpdateSuggestionStatusRequest
import com.terkula.uaxctf.statistics.response.SuggestionDTO
import com.terkula.uaxctf.statistics.response.SuggestionDetailResponse
import com.terkula.uaxctf.statistics.response.SuggestionsListResponse
import com.terkula.uaxctf.statistics.response.ThumbsUpResponse
import com.terkula.uaxctf.statistics.service.SuggestionService
import com.terkula.uaxctf.statisitcs.model.SuggestionComment
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class SuggestionController(
    val suggestionService: SuggestionService
) {

    @ApiOperation("Create a new suggestion")
    @PostMapping("/suggestions")
    fun createSuggestion(
        @RequestBody request: CreateSuggestionRequest
    ): SuggestionDTO {
        return suggestionService.createSuggestion(request)
    }

    @ApiOperation("Get all suggestions for a team")
    @GetMapping("/suggestions")
    fun getSuggestions(
        @ApiParam("Team name") @RequestParam team: String,
        @ApiParam("Optional runner ID to check thumbs up status") @RequestParam(required = false) runnerId: Int?
    ): SuggestionsListResponse {
        return suggestionService.getSuggestions(team, runnerId)
    }

    @ApiOperation("Get suggestion detail with comments")
    @GetMapping("/suggestions/{uuid}")
    fun getSuggestionDetail(
        @PathVariable uuid: String,
        @ApiParam("Optional runner ID to check thumbs up status") @RequestParam(required = false) runnerId: Int?
    ): SuggestionDetailResponse {
        return suggestionService.getSuggestionDetail(uuid, runnerId)
    }

    @ApiOperation("Update suggestion status (coaches only)")
    @PutMapping("/suggestions/status")
    fun updateStatus(
        @RequestBody request: UpdateSuggestionStatusRequest
    ): SuggestionDTO {
        // TODO: Add authentication check for coach role
        return suggestionService.updateStatus(request)
    }

    @ApiOperation("Toggle thumbs up on a suggestion")
    @PostMapping("/suggestions/thumbs-up")
    fun toggleThumbsUp(
        @RequestBody request: ToggleThumbsUpRequest
    ): ThumbsUpResponse {
        return suggestionService.toggleThumbsUp(request)
    }

    @ApiOperation("Add comment to suggestion")
    @PostMapping("/suggestions/comments")
    fun addComment(
        @RequestBody request: CreateSuggestionCommentRequest
    ): SuggestionComment {
        return suggestionService.addComment(request)
    }

    @ApiOperation("Delete suggestion (coaches only)")
    @DeleteMapping("/suggestions/{uuid}")
    fun deleteSuggestion(
        @PathVariable uuid: String
    ) {
        // TODO: Add authentication check for coach role
        suggestionService.deleteSuggestion(uuid)
    }
}
```

## API Endpoints Summary

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/api/suggestions` | Create new suggestion | Runner |
| GET | `/api/suggestions?team={team}&runnerId={id}` | List all suggestions | Any |
| GET | `/api/suggestions/{uuid}?runnerId={id}` | Get suggestion detail | Any |
| PUT | `/api/suggestions/status` | Update status | Coach |
| POST | `/api/suggestions/thumbs-up` | Toggle thumbs up | Runner |
| POST | `/api/suggestions/comments` | Add comment | Any |
| DELETE | `/api/suggestions/{uuid}` | Delete suggestion | Coach |

## Database Migration File

### Location: `db-migrations/add_suggestions_feature.sql`

```sql
-- Create suggestions table
CREATE TABLE IF NOT EXISTS uaxc.suggestions (
    id INT PRIMARY KEY AUTO_INCREMENT,
    uuid VARCHAR(255) UNIQUE NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(100) NOT NULL,
    runner_id INT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'under_review',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status_changed_at TIMESTAMP NULL,
    team VARCHAR(50) NOT NULL,
    season VARCHAR(10),
    INDEX idx_runner_id (runner_id),
    INDEX idx_status (status),
    INDEX idx_category (category),
    INDEX idx_created_at (created_at),
    INDEX idx_uuid (uuid),
    FOREIGN KEY (runner_id) REFERENCES uaxc.runners(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create suggestion thumbs up table
CREATE TABLE IF NOT EXISTS uaxc.suggestion_thumbs_up (
    id INT PRIMARY KEY AUTO_INCREMENT,
    suggestion_uuid VARCHAR(255) NOT NULL,
    runner_id INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_thumbs_up (suggestion_uuid, runner_id),
    INDEX idx_suggestion_uuid (suggestion_uuid),
    INDEX idx_runner_id (runner_id),
    FOREIGN KEY (runner_id) REFERENCES uaxc.runners(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create suggestion comments table
CREATE TABLE IF NOT EXISTS uaxc.suggestion_comments (
    id INT PRIMARY KEY AUTO_INCREMENT,
    uuid VARCHAR(255) UNIQUE NOT NULL,
    suggestion_uuid VARCHAR(255) NOT NULL,
    username VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_suggestion_uuid (suggestion_uuid),
    INDEX idx_created_at (created_at),
    INDEX idx_uuid (uuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

## Implementation Checklist

- [ ] Create database migration file: `db-migrations/add_suggestions_feature.sql`
- [ ] Run migration on database
- [ ] Create model classes:
  - [ ] `Suggestion.kt`
  - [ ] `SuggestionThumbsUp.kt`
  - [ ] `SuggestionComment.kt`
- [ ] Create repository interfaces:
  - [ ] `SuggestionRepository.kt`
  - [ ] `SuggestionThumbsUpRepository.kt`
  - [ ] `SuggestionCommentRepository.kt`
- [ ] Create request DTOs: `SuggestionRequests.kt`
- [ ] Create response DTOs: `SuggestionResponses.kt`
- [ ] Create service: `SuggestionService.kt`
- [ ] Create controller: `SuggestionController.kt`
- [ ] Test endpoints
- [ ] Add authentication/authorization checks for coach-only operations

## Notes

### Authentication/Authorization
The current codebase has a dual authentication system (Firebase + AppUser). For coach-only operations (update status, delete), you'll need to:
1. Pass the authenticated user info to the controller
2. Validate the user's role is "coach"
3. Return 403 Forbidden if unauthorized

Example authentication check pattern:
```kotlin
fun updateStatus(request: UpdateSuggestionStatusRequest, authenticatedUser: AppUser): SuggestionDTO {
    if (authenticatedUser.role != "coach") {
        throw UnauthorizedException("Only coaches can update suggestion status")
    }
    // ... rest of implementation
}
```

### Category Management
Categories are stored as strings. Consider creating an enum if you want strict validation:
```kotlin
enum class SuggestionCategory {
    FEATURE_REQUEST,
    BUG_REPORT,
    UI_UX,
    PERFORMANCE,
    OTHER
}
```

### Future Enhancements
- Email/push notifications when status changes
- Filter suggestions by status and category
- Analytics on most requested features
- Ability to mark suggestions as duplicates and link them
- Coach notes/feedback on rejected suggestions
