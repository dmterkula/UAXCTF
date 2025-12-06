package com.terkula.uaxctf.statistics.repository

import com.terkula.uaxctf.statisitcs.model.SeasonalReward
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

/**
 * Repository for SeasonalReward entities.
 * Provides CRUD operations and custom queries for seasonal reward management.
 */
@Repository
interface SeasonalRewardRepository : CrudRepository<SeasonalReward, Long> {

    /**
     * Find all rewards for a specific season/year, ordered by display order.
     * Used by team progress endpoint to show all configured rewards.
     *
     * @param season The season ("xc" or "track")
     * @param year The season year (e.g., "2024")
     * @return List of rewards ordered by display_order ASC
     */
    fun findBySeasonAndYearOrderByDisplayOrderAsc(season: String, year: String): List<SeasonalReward>

    /**
     * Find only achieved rewards for a specific season/year.
     * Ordered by achieved date (most recent first).
     *
     * @param season The season ("xc" or "track")
     * @param year The season year
     * @return List of achieved rewards ordered by achieved_date DESC
     */
    fun findBySeasonAndYearAndIsAchievedTrueOrderByAchievedDateDesc(
            season: String,
            year: String
    ): List<SeasonalReward>

    /**
     * Find only unachieved rewards for a specific season/year.
     * Used by auto-achievement logic to check which rewards to unlock.
     *
     * @param season The season ("xc" or "track")
     * @param year The season year
     * @return List of unachieved rewards ordered by display_order ASC
     */
    fun findBySeasonAndYearAndIsAchievedFalseOrderByDisplayOrderAsc(
            season: String,
            year: String
    ): List<SeasonalReward>

    /**
     * Check if a reward exists at a specific display order for a season/year.
     * Used to validate unique display orders before create/update operations.
     *
     * @param season The season ("xc" or "track")
     * @param year The season year
     * @param displayOrder The display order to check
     * @return true if a reward exists with this display order, false otherwise
     */
    fun existsBySeasonAndYearAndDisplayOrder(season: String, year: String, displayOrder: Int): Boolean
}
