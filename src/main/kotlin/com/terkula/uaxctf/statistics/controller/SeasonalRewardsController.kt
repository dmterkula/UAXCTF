package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statisitcs.model.SeasonalReward
import com.terkula.uaxctf.statistics.request.CreateSeasonalRewardRequest
import com.terkula.uaxctf.statistics.request.UpdateSeasonalRewardRequest
import com.terkula.uaxctf.statistics.response.RunnerSeasonalPointsResponse
import com.terkula.uaxctf.statistics.response.SeasonalProgressResponse
import com.terkula.uaxctf.statistics.response.SeasonalRewardDTO
import com.terkula.uaxctf.statistics.service.SeasonalRewardsService
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

/**
 * REST controller for seasonal rewards management.
 *
 * Base path: /api/v1/points/seasonal
 *
 * Endpoints:
 * - GET /progress - Get team progress with all rewards (public)
 * - GET /rewards - Get reward configurations (public)
 * - POST /rewards - Create new reward (coach only)
 * - PUT /rewards/{id} - Update reward (coach only)
 * - DELETE /rewards/{id} - Delete reward (coach only)
 * - GET /leaderboard - Get seasonal points leaderboard (public)
 * - GET /{runnerId} - Get runner's seasonal points (public)
 */
@RestController
@Validated
@RequestMapping("/api/v1/points/seasonal")
class SeasonalRewardsController(
        val seasonalRewardsService: SeasonalRewardsService
) {

    /**
     * Get team progress with all rewards and progress percentages.
     * Public endpoint - accessible to all users.
     *
     * Example: GET /api/v1/points/seasonal/progress?season=xc&year=2024
     */
    @ApiOperation("Get team progress with all rewards and progress percentages")
    @RequestMapping(value = ["/progress"], method = [RequestMethod.GET])
    fun getTeamProgress(
            @ApiParam("Season: xc or track", required = true)
            @RequestParam(value = "season", required = true) season: String,
            @ApiParam("Year (e.g., \"2024\")", required = true)
            @RequestParam(value = "year", required = true) year: String
    ): ResponseEntity<SeasonalProgressResponse> {
        val progress = seasonalRewardsService.getTeamProgress(season, year)
        return ResponseEntity.ok(progress)
    }

    /**
     * Get all seasonal rewards for a season/year.
     * Public endpoint - accessible to all users.
     *
     * Example: GET /api/v1/points/seasonal/rewards?season=xc&year=2024
     */
    @ApiOperation("Get all seasonal rewards for a season/year")
    @RequestMapping(value = ["/rewards"], method = [RequestMethod.GET])
    fun getRewards(
            @ApiParam("Season: xc or track", required = true)
            @RequestParam(value = "season", required = true) season: String,
            @ApiParam("Year (e.g., \"2024\")", required = true)
            @RequestParam(value = "year", required = true) year: String
    ): ResponseEntity<List<SeasonalRewardDTO>> {
        val rewards = seasonalRewardsService.getRewards(season, year)
        return ResponseEntity.ok(rewards)
    }

    /**
     * Create a new seasonal reward (coach only).
     *
     * TODO: Add authentication/authorization check for coach role.
     * For now, this endpoint is open but should be protected in production.
     *
     * Example:
     * POST /api/v1/points/seasonal/rewards
     * Body: {
     *   "season": "xc",
     *   "year": "2024",
     *   "rewardName": "Bronze Achievement",
     *   "description": "Team Pizza Party",
     *   "pointThreshold": 1000,
     *   "displayOrder": 1,
     *   "createdBy": "Coach Smith"
     * }
     */
    @ApiOperation("Create a new seasonal reward (coach only)")
    @RequestMapping(value = ["/rewards"], method = [RequestMethod.POST])
    fun createReward(
            @RequestBody request: CreateSeasonalRewardRequest
    ): ResponseEntity<SeasonalReward> {
        // TODO: Add authentication check
        // val appUser = extractAppUserFromAuth(authHeader)
        // validateCoachAccess(appUser)

        try {
            val reward = seasonalRewardsService.createReward(request)
            return ResponseEntity.ok(reward)
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.badRequest().build()
        }
    }

    /**
     * Update a seasonal reward (coach only).
     *
     * TODO: Add authentication/authorization check for coach role.
     *
     * Example:
     * PUT /api/v1/points/seasonal/rewards/123
     * Body: {
     *   "rewardName": "Bronze Achievement",
     *   "description": "Team Pizza Party (Updated)",
     *   "pointThreshold": 1200
     * }
     */
    @ApiOperation("Update a seasonal reward (coach only)")
    @RequestMapping(value = ["/rewards/{id}"], method = [RequestMethod.PUT])
    fun updateReward(
            @PathVariable("id") rewardId: Long,
            @RequestBody request: UpdateSeasonalRewardRequest
    ): ResponseEntity<SeasonalReward> {
        // TODO: Add authentication check
        // val appUser = extractAppUserFromAuth(authHeader)
        // validateCoachAccess(appUser)

        try {
            val reward = seasonalRewardsService.updateReward(rewardId, request)
            return ResponseEntity.ok(reward)
        } catch (e: RuntimeException) {
            // Reward not found
            return ResponseEntity.notFound().build()
        } catch (e: IllegalArgumentException) {
            // Display order conflict
            return ResponseEntity.badRequest().build()
        }
    }

    /**
     * Delete a seasonal reward (coach only).
     *
     * TODO: Add authentication/authorization check for coach role.
     *
     * Example: DELETE /api/v1/points/seasonal/rewards/123
     */
    @ApiOperation("Delete a seasonal reward (coach only)")
    @RequestMapping(value = ["/rewards/{id}"], method = [RequestMethod.DELETE])
    fun deleteReward(
            @PathVariable("id") rewardId: Long
    ): ResponseEntity<Void> {
        // TODO: Add authentication check
        // val appUser = extractAppUserFromAuth(authHeader)
        // validateCoachAccess(appUser)

        try {
            seasonalRewardsService.deleteReward(rewardId)
            return ResponseEntity.ok().build()
        } catch (e: RuntimeException) {
            // Reward not found
            return ResponseEntity.notFound().build()
        }
    }

    /**
     * Get a specific runner's seasonal points.
     * Public endpoint - accessible to all users.
     *
     * Example: GET /api/v1/points/seasonal/123?season=xc&year=2024
     */
    @ApiOperation("Get a specific runner's seasonal points")
    @RequestMapping(value = ["/{runnerId}"], method = [RequestMethod.GET])
    fun getRunnerSeasonalPoints(
            @PathVariable("runnerId") runnerId: Int,
            @ApiParam("Season: xc or track", required = true)
            @RequestParam(value = "season", required = true) season: String,
            @ApiParam("Year (e.g., \"2024\")", required = true)
            @RequestParam(value = "year", required = true) year: String
    ): ResponseEntity<RunnerSeasonalPointsResponse> {
        val points = seasonalRewardsService.getRunnerSeasonalPoints(runnerId, season, year)
        return ResponseEntity.ok(
                RunnerSeasonalPointsResponse(
                        runnerId = runnerId,
                        season = season,
                        year = year,
                        pointsEarned = points
                )
        )
    }

    /**
     * Get seasonal points leaderboard.
     * Returns runners sorted by points earned (highest first).
     * Public endpoint - accessible to all users.
     *
     * Example: GET /api/v1/points/seasonal/leaderboard?season=xc&year=2024&limit=10
     */
    @ApiOperation("Get seasonal points leaderboard")
    @RequestMapping(value = ["/leaderboard"], method = [RequestMethod.GET])
    fun getSeasonalLeaderboard(
            @ApiParam("Season: xc or track", required = true)
            @RequestParam(value = "season", required = true) season: String,
            @ApiParam("Year (e.g., \"2024\")", required = true)
            @RequestParam(value = "year", required = true) year: String,
            @ApiParam("Optional limit on number of results (e.g., 10 for top 10)", required = false)
            @RequestParam(value = "limit", required = false) limit: Int?
    ): ResponseEntity<List<RunnerSeasonalPointsResponse>> {
        val leaderboard = seasonalRewardsService.getSeasonalLeaderboard(season, year, limit)

        val response = leaderboard.map { tracking ->
            RunnerSeasonalPointsResponse(
                    runnerId = tracking.runnerId,
                    season = tracking.season,
                    year = tracking.year,
                    pointsEarned = tracking.pointsEarned
            )
        }

        return ResponseEntity.ok(response)
    }

    // ===== Authorization Helper Methods (TODO: Implement) =====

    // TODO: Implement these methods based on existing authentication pattern
    // See AuthenticationController for reference

    // private fun extractAppUserFromAuth(authHeader: String?): AppUser {
    //     if (authHeader == null || authHeader.isEmpty()) {
    //         throw UnauthenticatedException("Authorization header required")
    //     }
    //     // Extract and validate user from auth header
    //     // Return AppUser object
    //     throw NotImplementedError("Auth extraction not implemented")
    // }

    // private fun validateCoachAccess(appUser: AppUser) {
    //     if (appUser.role != "coach") {
    //         throw UnauthenticatedException("Only coaches can manage seasonal rewards")
    //     }
    // }
}
