package com.terkula.uaxctf.statistics.repository

import com.terkula.uaxctf.statisitcs.model.SeasonalPointsTracking
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

/**
 * Repository for SeasonalPointsTracking entities.
 * Provides CRUD operations and custom queries for seasonal points management.
 */
@Repository
interface SeasonalPointsTrackingRepository : CrudRepository<SeasonalPointsTracking, Long> {

    /**
     * Find a specific runner's seasonal points record.
     * Used when updating seasonal points (upsert pattern).
     *
     * @param runnerId The runner's ID
     * @param season The season ("xc" or "track")
     * @param year The season year
     * @return Optional containing the tracking record if exists
     */
    fun findByRunnerIdAndSeasonAndYear(
            runnerId: Int,
            season: String,
            year: String
    ): Optional<SeasonalPointsTracking>

    /**
     * Get all tracking records for a season/year.
     * Can be used for seasonal leaderboards or reporting.
     *
     * @param season The season ("xc" or "track")
     * @param year The season year
     * @return List of all tracking records for the season
     */
    fun findBySeasonAndYear(season: String, year: String): List<SeasonalPointsTracking>

    /**
     * Calculate total team points for a season/year.
     * Sums points_earned only for the specified runner IDs (active roster).
     *
     * This is a critical performance optimization - instead of loading all entities
     * and summing in-memory, we let the database do the aggregation.
     *
     * @param season The season ("xc" or "track")
     * @param year The season year
     * @param runnerIds List of runner IDs to include (active roster only)
     * @return Sum of points_earned for specified runners, or 0 if none exist
     */
    @Query("""
        SELECT COALESCE(SUM(spt.pointsEarned), 0)
        FROM SeasonalPointsTracking spt
        WHERE spt.season = :season
        AND spt.year = :year
        AND spt.runnerId IN :runnerIds
    """)
    fun calculateTeamTotal(
            @Param("season") season: String,
            @Param("year") year: String,
            @Param("runnerIds") runnerIds: List<Int>
    ): Int

    /**
     * Get seasonal leaderboard - all runners sorted by points earned (descending).
     * Optional feature for future enhancement.
     *
     * @param season The season ("xc" or "track")
     * @param year The season year
     * @return List of tracking records ordered by points_earned DESC
     */
    fun findBySeasonAndYearOrderByPointsEarnedDesc(season: String, year: String): List<SeasonalPointsTracking>
}
