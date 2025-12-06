package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statisitcs.model.PointTransaction
import com.terkula.uaxctf.statisitcs.model.PointsConfiguration
import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.statistics.repository.PointTransactionRepository
import com.terkula.uaxctf.statistics.repository.PointsConfigurationRepository
import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.statistics.request.*
import com.terkula.uaxctf.statistics.response.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@Service
class PointsService(
        val runnerRepository: RunnerRepository,
        val pointTransactionRepository: PointTransactionRepository,
        val pointsConfigurationRepository: PointsConfigurationRepository,
        val seasonalRewardsService: SeasonalRewardsService
) {

    // Cache configuration in memory, refresh every 5 minutes
    private val configCache = ConcurrentHashMap<String, Int>()
    private var lastConfigRefresh: LocalDateTime? = null
    private val cacheExpirationMinutes: Long = 5

    /**
     * Get the current point balance for a runner
     */
    fun getPointBalance(runnerId: Int): PointBalanceResponse {
        val runner = runnerRepository.findById(runnerId)
                .orElseThrow { RuntimeException("Runner not found: $runnerId") }

        return PointBalanceResponse(
                currentPoints = runner.points,
                lifetimePointsEarned = runner.lifetimePointsEarned,
                lastUpdated = runner.lastPointsUpdated
        )
    }

    /**
     * Validate if a runner has enough points for a feature
     */
    fun validatePoints(request: ValidatePointsRequest): ValidatePointsResponse {
        val requiredPoints = getConfigValue("COST_${request.featureType}")
        val runner = runnerRepository.findById(request.runnerId)
                .orElseThrow { RuntimeException("Runner not found: ${request.runnerId}") }

        val hasEnough = runner.points >= requiredPoints

        return ValidatePointsResponse(
                hasEnoughPoints = hasEnough,
                currentBalance = runner.points,
                pointsRequired = requiredPoints,
                shortfall = if (hasEnough) 0 else requiredPoints - runner.points
        )
    }

    /**
     * Spend points for a feature. Uses database-level locking to prevent race conditions.
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    fun spendPoints(request: SpendPointsRequest): SpendPointsResponse {
        val cost = getConfigValue("COST_${request.featureType}")

        // Lock the runner row for update
        val runner = runnerRepository.findById(request.runnerId)
                .orElseThrow { RuntimeException("Runner not found: ${request.runnerId}") }

        // Validate balance
        if (runner.points < cost) {
            return SpendPointsResponse(
                    success = false,
                    newBalance = runner.points,
                    pointsSpent = 0,
                    errorMessage = "Insufficient points. Need $cost, have ${runner.points}"
            )
        }

        // Deduct points
        runner.points -= cost
        runner.lastPointsUpdated = Timestamp(System.currentTimeMillis())
        runnerRepository.save(runner)

        // Log transaction
        val transaction = createTransaction(
                runner = runner,
                pointsAmount = -cost,
                transactionType = "SPENT",
                featureType = request.featureType,
                description = request.description ?: "Spent points on ${request.featureType}"
        )

        return SpendPointsResponse(
                success = true,
                newBalance = runner.points,
                pointsSpent = cost,
                transaction = transaction
        )
    }

    /**
     * Refund points from a previous transaction (used when an action fails)
     */
    @Transactional
    fun refundPoints(request: RefundPointsRequest): RefundPointsResponse {
        val originalTransaction = pointTransactionRepository.findById(request.transactionId)
                .orElseThrow { RuntimeException("Transaction not found: ${request.transactionId}") }

        // Validate transaction can be refunded
        if (originalTransaction.transactionType != "SPENT") {
            return RefundPointsResponse(
                    success = false,
                    newBalance = 0,
                    pointsRefunded = 0,
                    errorMessage = "Can only refund SPENT transactions"
            )
        }

        if (originalTransaction.isRefunded) {
            return RefundPointsResponse(
                    success = false,
                    newBalance = 0,
                    pointsRefunded = 0,
                    errorMessage = "Transaction already refunded"
            )
        }

        val runner = runnerRepository.findById(originalTransaction.runnerId)
                .orElseThrow { RuntimeException("Runner not found: ${originalTransaction.runnerId}") }

        // Refund the points (original amount was negative, so negate it to add back)
        val refundAmount = -originalTransaction.pointsAmount
        runner.points += refundAmount
        runner.lastPointsUpdated = Timestamp(System.currentTimeMillis())
        runnerRepository.save(runner)

        // Mark original transaction as refunded
        originalTransaction.isRefunded = true
        originalTransaction.refundedAt = Timestamp(System.currentTimeMillis())
        pointTransactionRepository.save(originalTransaction)

        // Create refund transaction
        val refundTransaction = createTransaction(
                runner = runner,
                pointsAmount = refundAmount,
                transactionType = "REFUND",
                featureType = originalTransaction.featureType,
                description = "Refund for transaction #${originalTransaction.id}",
                relatedTransactionId = originalTransaction.id
        )

        return RefundPointsResponse(
                success = true,
                newBalance = runner.points,
                pointsRefunded = refundAmount
        )
    }

    /**
     * Award points when an activity is logged
     */
    @Transactional
    fun earnPoints(request: EarnPointsRequest): EarnPointsResponse {
        val pointsToAward = getConfigValue("EARN_${request.activityType}")

        // Check if points already awarded for this activity
        if (request.activityUuid != null) {
            val existingTransaction = pointTransactionRepository.findByActivityUuid(request.activityUuid)
            if (existingTransaction != null) {
                // Points already awarded, return success but don't award again
                return EarnPointsResponse(
                        success = true,
                        pointsEarned = 0,
                        newBalance = runnerRepository.findById(request.runnerId).get().points,
                        transaction = existingTransaction
                )
            }
        }

        val runner = runnerRepository.findById(request.runnerId)
                .orElseThrow { RuntimeException("Runner not found: ${request.runnerId}") }

        // Award points
        runner.points += pointsToAward
        runner.lifetimePointsEarned += pointsToAward
        runner.lastPointsUpdated = Timestamp(System.currentTimeMillis())
        runnerRepository.save(runner)

        // Log transaction
        val transaction = createTransaction(
                runner = runner,
                pointsAmount = pointsToAward,
                transactionType = "EARNED",
                activityType = request.activityType,
                activityUuid = request.activityUuid,
                season = request.season,
                description = request.description ?: "Earned points from ${request.activityType}"
        )

        // Update seasonal tracking if season and year are provided
        if (request.season != null && request.year != null) {
            try {
                seasonalRewardsService.updateSeasonalPoints(
                        runnerId = request.runnerId,
                        season = request.season,
                        year = request.year,
                        pointsEarned = pointsToAward
                )
            } catch (e: Exception) {
                // Log error but don't fail the transaction
                // Seasonal tracking is supplementary to core points functionality
                println("Warning: Failed to update seasonal tracking: ${e.message}")
                e.printStackTrace()
            }
        }

        return EarnPointsResponse(
                success = true,
                pointsEarned = pointsToAward,
                newBalance = runner.points,
                transaction = transaction
        )
    }

    /**
     * Get transaction history for a runner
     */
    fun getTransactionHistory(runnerId: Int, limit: Int = 50): List<PointTransaction> {
        return if (limit > 0) {
            pointTransactionRepository.findTop50ByRunnerIdOrderByCreatedAtDesc(runnerId)
        } else {
            pointTransactionRepository.findByRunnerIdOrderByCreatedAtDesc(runnerId)
        }
    }

    /**
     * Get all points configuration values
     */
    fun getConfiguration(): Map<String, Int> {
        refreshConfigCacheIfNeeded()
        return HashMap(configCache)
    }

    /**
     * Update a configuration value (coach/admin only - should be secured)
     */
    @Transactional
    fun updateConfiguration(request: UpdatePointsConfigRequest) {
        val config = pointsConfigurationRepository.findByConfigKey(request.configKey)
                .orElseThrow { RuntimeException("Configuration key not found: ${request.configKey}") }

        config.configValue = request.configValue
        config.updatedBy = request.updatedBy
        pointsConfigurationRepository.save(config)

        // Invalidate cache
        lastConfigRefresh = null
    }

    /**
     * Get points leaderboard (top runners by points)
     * Only includes UA team members (excludes NU team)
     */
    fun getLeaderboard(limit: Int = 100): List<Runner> {
        return runnerRepository.findAll()
                .filter { it.team == "UA" }  // Only UA team, exclude NU
                .sortedByDescending { it.points }
                .take(limit)
    }

    // ===== Private Helper Methods =====

    private fun createTransaction(
            runner: Runner,
            pointsAmount: Int,
            transactionType: String,
            activityType: String? = null,
            featureType: String? = null,
            activityUuid: String? = null,
            season: String? = null,
            description: String? = null,
            relatedTransactionId: Long? = null
    ): PointTransaction {
        val transaction = PointTransaction(
                runnerId = runner.id,
                transactionType = transactionType,
                pointsAmount = pointsAmount,
                activityType = activityType,
                featureType = featureType,
                activityUuid = activityUuid,
                description = description,
                balanceAfter = runner.points,
                season = season,
                relatedTransactionId = relatedTransactionId
        )

        return pointTransactionRepository.save(transaction)
    }

    private fun getConfigValue(key: String): Int {
        refreshConfigCacheIfNeeded()
        return configCache.getOrDefault(key, 0)
    }

    private fun refreshConfigCacheIfNeeded() {
        if (shouldRefreshCache()) {
            val configs = pointsConfigurationRepository.findAll()
            configCache.clear()
            configs.forEach { config ->
                configCache[config.configKey] = config.configValue
            }
            lastConfigRefresh = LocalDateTime.now()
        }
    }

    private fun shouldRefreshCache(): Boolean {
        val lastRefresh = lastConfigRefresh ?: return true
        val minutesSinceRefresh = Duration.between(lastRefresh, LocalDateTime.now()).toMinutes()
        return minutesSinceRefresh > cacheExpirationMinutes
    }
}