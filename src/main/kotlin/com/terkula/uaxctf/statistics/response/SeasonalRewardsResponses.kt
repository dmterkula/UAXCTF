package com.terkula.uaxctf.statistics.response

import java.sql.Timestamp

/**
 * DTO for a seasonal reward with optional progress percentage.
 * Used in API responses to separate entity representation from API contract.
 */
data class SeasonalRewardDTO(
        val id: Long,
        val season: String,              // "xc" or "track"
        val year: String,
        val rewardName: String,
        val description: String,
        val pointThreshold: Int,
        val displayOrder: Int,
        val isAchieved: Boolean,
        val achievedDate: Timestamp?,
        val progressPercentage: Double? = null  // Only populated for progress endpoint
)

/**
 * Response for team progress endpoint.
 * Includes team totals, active roster count, and all rewards with progress percentages.
 */
data class SeasonalProgressResponse(
        val season: String,              // "xc" or "track"
        val year: String,
        val teamTotalPoints: Int,        // Sum of all active roster members' seasonal points
        val activeRosterCount: Int,      // Number of active athletes on roster
        val rewards: List<SeasonalRewardDTO>  // All rewards with progress percentages
)

/**
 * Response for individual runner's seasonal points.
 */
data class RunnerSeasonalPointsResponse(
        val runnerId: Int,
        val season: String,              // "xc" or "track"
        val year: String,
        val pointsEarned: Int            // Points earned this season
)
