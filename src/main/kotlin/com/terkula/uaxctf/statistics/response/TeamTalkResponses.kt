package com.terkula.uaxctf.statistics.response

import com.terkula.uaxctf.statisitcs.model.TeamTalk
import com.terkula.uaxctf.statisitcs.model.TeamTalkComment

/**
 * Aggregated reaction count per emoji
 */
data class ReactionSummary(
    val emoji: String,
    val count: Int,
    val usernames: List<String>  // List of usernames who reacted
)

/**
 * Nested comment structure for display
 * Supports recursive nesting for threaded discussions
 * Matches iOS NestedComment struct
 */
data class NestedComment(
    val uuid: String,
    val teamTalkUuid: String,
    val username: String,  // Unique identifier AND cache key for profile pictures
    val displayName: String,  // runner.name for runners, user.username for coaches
    val deviceId: String? = null,  // Device ID for push notifications
    val message: String,
    val createdAt: String,  // ISO 8601 string format for iOS
    val updatedAt: String,  // ISO 8601 string format for iOS
    val replies: List<NestedComment> = emptyList()
)

/**
 * Summary of views for a team talk
 * Only exposed to coaches on frontend
 */
data class ViewSummary(
    val totalViews: Long,
    val uniqueViewers: Int,
    val recentViews: List<ViewDetail>
)

/**
 * Individual view detail
 */
data class ViewDetail(
    val username: String,
    val displayName: String,
    val team: String,
    val viewedAt: String
)

/**
 * Complete team talk with all associated data
 */
data class TeamTalkResponse(
    val teamTalk: TeamTalk,
    val reactions: List<ReactionSummary>,
    val comments: List<NestedComment>,
    val totalCommentCount: Int,
    val viewSummary: ViewSummary
)

/**
 * List response for multiple team talks
 */
data class TeamTalkListResponse(
    val teamTalks: List<TeamTalkResponse>
)

/**
 * Response when creating a comment
 * Includes points information for immediate UI update
 */
data class CommentCreatedResponse(
    val comment: TeamTalkComment,
    val pointsEarned: Int,
    val newPointBalance: Int
)
