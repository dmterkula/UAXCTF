package com.terkula.uaxctf.statistics.request

/**
 * Request to create a new seasonal reward configuration.
 * Used by coaches to set up team-based reward tiers.
 */
data class CreateSeasonalRewardRequest(
        val season: String,           // "xc" or "track"
        val year: String,             // Season year (e.g., "2024", "2025")
        val rewardName: String,       // Name of the reward (e.g., "Bronze Tier", "Gold Achievement")
        val description: String,      // Reward description (e.g., "Team Pizza Party")
        val pointThreshold: Int,      // Points required to unlock this reward
        val displayOrder: Int,        // Display order for UI (1, 2, 3...)
        val createdBy: String? = null // Coach who created the reward (optional)
)

/**
 * Request to update an existing seasonal reward.
 * All fields are optional - only provided fields will be updated.
 */
data class UpdateSeasonalRewardRequest(
        val rewardName: String? = null,       // Updated reward name
        val description: String? = null,      // Updated description
        val pointThreshold: Int? = null,      // Updated point threshold
        val displayOrder: Int? = null         // Updated display order
)
