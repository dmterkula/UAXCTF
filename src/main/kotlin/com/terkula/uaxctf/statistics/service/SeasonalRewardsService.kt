package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statisitcs.model.SeasonalPointsTracking
import com.terkula.uaxctf.statisitcs.model.SeasonalReward
import com.terkula.uaxctf.statistics.repository.SeasonalPointsTrackingRepository
import com.terkula.uaxctf.statistics.repository.SeasonalRewardRepository
import com.terkula.uaxctf.statistics.request.CreateSeasonalRewardRequest
import com.terkula.uaxctf.statistics.request.UpdateSeasonalRewardRequest
import com.terkula.uaxctf.statistics.response.SeasonalProgressResponse
import com.terkula.uaxctf.statistics.response.SeasonalRewardDTO
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp

/**
 * Service for managing seasonal rewards and tracking team progress.
 *
 * Key responsibilities:
 * - Update seasonal points when points are earned
 * - Calculate team progress (sum of active roster members' points)
 * - Auto-achieve rewards when team reaches thresholds
 * - CRUD operations for reward configurations
 */
@Service
class SeasonalRewardsService(
        val seasonalRewardRepository: SeasonalRewardRepository,
        val seasonalPointsTrackingRepository: SeasonalPointsTrackingRepository,
        val runnerService: RunnerService
) {

    /**
     * Update seasonal points tracking when a runner earns points.
     * Called from PointsService.earnPoints() after awarding points.
     *
     * Performs an upsert operation:
     * - If tracking record exists, increment points_earned
     * - If not, create new record with initial points
     *
     * After updating, triggers auto-achievement check.
     *
     * @param runnerId The runner who earned points
     * @param season The season ("xc" or "track")
     * @param year The season year
     * @param pointsEarned The amount of points earned
     */
    @Transactional
    fun updateSeasonalPoints(runnerId: Int, season: String, year: String, pointsEarned: Int) {
        // Find or create seasonal tracking record (upsert pattern)
        val tracking = seasonalPointsTrackingRepository
                .findByRunnerIdAndSeasonAndYear(runnerId, season, year)
                .orElse(
                        SeasonalPointsTracking(
                                runnerId = runnerId,
                                season = season,
                                year = year,
                                pointsEarned = 0
                        )
                )

        // Increment points
        tracking.pointsEarned += pointsEarned
        seasonalPointsTrackingRepository.save(tracking)

        // Check if any rewards should be auto-achieved
        checkAndAchieveRewards(season, year)
    }

    /**
     * Calculate team progress for a season/year.
     * Returns total team points and all rewards with progress percentages.
     *
     * Team total = Sum of seasonal points for all ACTIVE UA team roster members only.
     * Inactive runners and NU team members are excluded from the calculation.
     *
     * @param season The season ("xc" or "track")
     * @param year The season year
     * @return SeasonalProgressResponse with team total, active count, and rewards
     */
    fun getTeamProgress(season: String, year: String): SeasonalProgressResponse {
        // Get active roster based on season type (UA team only)
        val activeRunners = if (season == "xc") {
            runnerService.getXcRoster(active = true, season = year)
        } else {
            runnerService.getTrackRoster(active = true, season = year)
        }.filter { it.team == "UA" }  // Only UA team, exclude NU

        val runnerIds = activeRunners.map { it.id }

        // Calculate total team points (sum of all active roster members)
        val teamTotal = if (runnerIds.isEmpty()) {
            0
        } else {
            seasonalPointsTrackingRepository.calculateTeamTotal(season, year, runnerIds)
        }

        // Get all rewards for this season with progress percentages
        val rewards = seasonalRewardRepository
                .findBySeasonAndYearOrderByDisplayOrderAsc(season, year)
                .map { reward ->
                    SeasonalRewardDTO(
                            id = reward.id,
                            season = reward.season,
                            year = reward.year,
                            rewardName = reward.rewardName,
                            description = reward.description,
                            pointThreshold = reward.pointThreshold,
                            displayOrder = reward.displayOrder,
                            isAchieved = reward.isAchieved,
                            achievedDate = reward.achievedDate,
                            progressPercentage = calculateProgressPercentage(teamTotal, reward.pointThreshold)
                    )
                }

        return SeasonalProgressResponse(
                season = season,
                year = year,
                teamTotalPoints = teamTotal,
                activeRosterCount = activeRunners.size,
                rewards = rewards
        )
    }

    /**
     * Get all rewards for a season/year (without progress percentages).
     * Used by reward configuration endpoint.
     *
     * @param season The season ("xc" or "track")
     * @param year The season year
     * @return List of reward DTOs
     */
    fun getRewards(season: String, year: String): List<SeasonalRewardDTO> {
        return seasonalRewardRepository
                .findBySeasonAndYearOrderByDisplayOrderAsc(season, year)
                .map { reward ->
                    SeasonalRewardDTO(
                            id = reward.id,
                            season = reward.season,
                            year = reward.year,
                            rewardName = reward.rewardName,
                            description = reward.description,
                            pointThreshold = reward.pointThreshold,
                            displayOrder = reward.displayOrder,
                            isAchieved = reward.isAchieved,
                            achievedDate = reward.achievedDate,
                            progressPercentage = null // Not calculated for simple reward list
                    )
                }
    }

    /**
     * Create a new seasonal reward (coach only).
     * Validates that display order is unique for the season/year.
     *
     * @param request Create request with reward details
     * @return The created SeasonalReward entity
     * @throws IllegalArgumentException if display order already exists
     */
    @Transactional
    fun createReward(request: CreateSeasonalRewardRequest): SeasonalReward {
        // Validate display order is unique
        if (seasonalRewardRepository.existsBySeasonAndYearAndDisplayOrder(
                        request.season, request.year, request.displayOrder
                )) {
            throw IllegalArgumentException(
                    "Reward with display order ${request.displayOrder} already exists for ${request.season} ${request.year}"
            )
        }

        val reward = SeasonalReward(
                season = request.season,
                year = request.year,
                rewardName = request.rewardName,
                description = request.description,
                pointThreshold = request.pointThreshold,
                displayOrder = request.displayOrder,
                createdBy = request.createdBy
        )

        return seasonalRewardRepository.save(reward)
    }

    /**
     * Update an existing reward (coach only).
     * Validates display order uniqueness if being changed.
     *
     * @param rewardId The reward ID to update
     * @param request Update request with optional fields
     * @return The updated SeasonalReward entity
     * @throws RuntimeException if reward not found
     * @throws IllegalArgumentException if new display order conflicts
     */
    @Transactional
    fun updateReward(rewardId: Long, request: UpdateSeasonalRewardRequest): SeasonalReward {
        val reward = seasonalRewardRepository.findById(rewardId)
                .orElseThrow { RuntimeException("Reward not found: $rewardId") }

        // Update mutable fields if provided
        request.rewardName?.let { reward.rewardName = it }
        request.description?.let { reward.description = it }
        request.pointThreshold?.let { reward.pointThreshold = it }
        request.displayOrder?.let { newOrder ->
            // Check if new display order conflicts with another reward
            if (newOrder != reward.displayOrder &&
                    seasonalRewardRepository.existsBySeasonAndYearAndDisplayOrder(
                            reward.season, reward.year, newOrder
                    )) {
                throw IllegalArgumentException(
                        "Reward with display order $newOrder already exists for ${reward.season} ${reward.year}"
                )
            }
            reward.displayOrder = newOrder
        }

        return seasonalRewardRepository.save(reward)
    }

    /**
     * Delete a reward (coach only).
     *
     * @param rewardId The reward ID to delete
     * @throws RuntimeException if reward not found
     */
    @Transactional
    fun deleteReward(rewardId: Long) {
        if (!seasonalRewardRepository.existsById(rewardId)) {
            throw RuntimeException("Reward not found: $rewardId")
        }
        seasonalRewardRepository.deleteById(rewardId)
    }

    /**
     * Get a specific runner's seasonal points.
     *
     * @param runnerId The runner's ID
     * @param season The season ("xc" or "track")
     * @param year The season year
     * @return Points earned this season, or 0 if no record exists
     */
    fun getRunnerSeasonalPoints(runnerId: Int, season: String, year: String): Int {
        return seasonalPointsTrackingRepository
                .findByRunnerIdAndSeasonAndYear(runnerId, season, year)
                .map { it.pointsEarned }
                .orElse(0)
    }

    /**
     * Get seasonal points leaderboard for all runners.
     * Returns only runners who have earned points this season, sorted by points descending.
     *
     * @param season The season ("xc" or "track")
     * @param year The season year
     * @param limit Optional limit on number of results (e.g., top 10). If null, returns all.
     * @return List of seasonal tracking records sorted by points (highest first)
     */
    fun getSeasonalLeaderboard(season: String, year: String, limit: Int?): List<SeasonalPointsTracking> {
        val allResults = seasonalPointsTrackingRepository
                .findBySeasonAndYearOrderByPointsEarnedDesc(season, year)

        return if (limit != null && limit > 0) {
            allResults.take(limit)
        } else {
            allResults
        }
    }

    // ===== Private Helper Methods =====

    /**
     * Check if team total has reached any unachieved reward thresholds.
     * Auto-marks rewards as achieved when threshold is met.
     *
     * This is called automatically after updating seasonal points.
     * Only counts UA team members toward team total (excludes NU team).
     *
     * @param season The season to check
     * @param year The season year
     */
    @Transactional
    private fun checkAndAchieveRewards(season: String, year: String) {
        // Get current team total (UA team only)
        val activeRunners = if (season == "xc") {
            runnerService.getXcRoster(active = true, season = year)
        } else {
            runnerService.getTrackRoster(active = true, season = year)
        }.filter { it.team == "UA" }  // Only UA team, exclude NU

        if (activeRunners.isEmpty()) return

        val runnerIds = activeRunners.map { it.id }
        val teamTotal = seasonalPointsTrackingRepository.calculateTeamTotal(season, year, runnerIds)

        // Get all unachieved rewards
        val unachievedRewards = seasonalRewardRepository
                .findBySeasonAndYearAndIsAchievedFalseOrderByDisplayOrderAsc(season, year)

        // Mark rewards as achieved if threshold is met
        unachievedRewards.forEach { reward ->
            if (teamTotal >= reward.pointThreshold) {
                reward.isAchieved = true
                reward.achievedDate = Timestamp(System.currentTimeMillis())
                seasonalRewardRepository.save(reward)

                // TODO: Future enhancement - trigger notification/celebration
                println("🏆 Team unlocked reward: ${reward.description} (${reward.season} ${reward.year})")
            }
        }
    }

    /**
     * Calculate progress percentage toward a reward threshold.
     *
     * @param current Current team points
     * @param target Target threshold
     * @return Progress percentage (0.0 to 100.0), capped at 100%
     */
    private fun calculateProgressPercentage(current: Int, target: Int): Double {
        if (target <= 0) return 0.0
        val percentage = (current.toDouble() / target.toDouble()) * 100.0
        return minOf(percentage, 100.0) // Cap at 100%
    }
}
