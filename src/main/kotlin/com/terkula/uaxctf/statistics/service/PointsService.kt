package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statisitcs.model.PointTransaction
import com.terkula.uaxctf.statisitcs.model.PointsConfiguration
import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.statisitcs.model.SummerConditioningConfig
import com.terkula.uaxctf.statistics.repository.PointTransactionRepository
import com.terkula.uaxctf.statistics.repository.PointsConfigurationRepository
import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.statistics.repository.SummerConditioningConfigRepository
import com.terkula.uaxctf.statistics.request.*
import com.terkula.uaxctf.statistics.response.*
import com.terkula.uaxctf.statistics.service.RunnerService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import java.sql.Date
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Duration
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

@Service
class PointsService(
        val runnerRepository: RunnerRepository,
        val pointTransactionRepository: PointTransactionRepository,
        val pointsConfigurationRepository: PointsConfigurationRepository,
        val seasonalRewardsService: SeasonalRewardsService,
        val summerConditioningConfigRepository: SummerConditioningConfigRepository,
        val runnerService: RunnerService,
        val summerTrainingAwardService: com.terkula.uaxctf.training.service.SummerTrainingAwardService
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
     * Re-attribute existing point transactions from one season to another.
     * Called when a TrainingRun's season label is corrected after runners have already logged it.
     * Only EARNED transactions for the given activity UUIDs are updated.
     */
    @Transactional
    fun reattributePointsForSeasonChange(activityUuids: List<String>, oldSeason: String, newSeason: String, runDate: java.sql.Date) {
        if (activityUuids.isEmpty() || oldSeason == newSeason) return

        val oldYear = computeYearForSeason(oldSeason, runDate)
        val newYear = computeYearForSeason(newSeason, runDate)

        val transactions = pointTransactionRepository.findByActivityUuidIn(activityUuids)
                .filter { it.transactionType == "EARNED" && it.season == oldSeason }

        transactions.forEach { txn ->
            txn.season = newSeason
            pointTransactionRepository.save(txn)
            seasonalRewardsService.transferSeasonalPoints(
                    runnerId = txn.runnerId,
                    fromSeason = oldSeason,
                    fromYear = oldYear,
                    toSeason = newSeason,
                    toYear = newYear,
                    points = txn.pointsAmount
            )
        }
    }

    private fun computeYearForSeason(season: String, date: java.sql.Date): String {
        val localDate = date.toLocalDate()
        val month = localDate.monthValue
        val year = localDate.year
        return if (season == "track" && month >= 11) (year + 1).toString() else year.toString()
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
     * Get summer conditioning points for all active XC runners for a given year.
     * Uses the stored config window for that year; the end date is capped at today
     * if the window has not yet closed, so future points never bleed in.
     */
    fun getSummerConditioningPoints(year: String): SummerConditioningResponse {
        val config = summerConditioningConfigRepository.findByYear(year)
                .orElseThrow { RuntimeException("No summer conditioning config found for year $year") }

        val (startTs, endTs, windowClosed) = resolveDateWindow(config)

        val transactions = pointTransactionRepository.findByTransactionTypeAndCreatedAtBetween("EARNED", startTs, endTs)
        val pointsByRunner = transactions.groupBy { it.runnerId }.mapValues { (_, txns) -> txns.sumOf { it.pointsAmount } }

        val runners = runnerService.getXcRoster(true, year)
                .filter { it.team == "UA" }
                .map { runner -> RunnerSummerConditioningDTO(runner = runner, pointsEarned = pointsByRunner.getOrDefault(runner.id, 0), pointsThreshold = config.pointsThreshold) }
                .sortedByDescending { it.pointsEarned }

        return SummerConditioningResponse(
                year = year,
                startDate = config.startDate.toString(),
                endDate = config.endDate.toString(),
                pointsThreshold = config.pointsThreshold,
                windowClosed = windowClosed,
                runners = runners
        )
    }

    fun getSummerConditioningPointsForRunner(runnerId: Int, year: String): RunnerSummerConditioningDTO? {
        val config = summerConditioningConfigRepository.findByYear(year).orElse(null) ?: return null
        val runner = runnerRepository.findById(runnerId).orElse(null) ?: return null

        val (startTs, endTs, _) = resolveDateWindow(config)
        val pointsEarned = pointTransactionRepository
                .findByRunnerIdAndTransactionTypeAndCreatedAtBetween(runnerId, "EARNED", startTs, endTs)
                .sumOf { it.pointsAmount }

        return RunnerSummerConditioningDTO(runner = runner, pointsEarned = pointsEarned, pointsThreshold = config.pointsThreshold)
    }

    fun getSummerConditioningBreakdown(runnerId: Int, year: String): SummerConditioningBreakdownResponse {
        val config = summerConditioningConfigRepository.findByYear(year)
                .orElseThrow { RuntimeException("No summer conditioning config for year $year") }

        val (startTs, endTs, _) = resolveDateWindow(config)

        val transactions = pointTransactionRepository
                .findByRunnerIdAndTransactionTypeAndCreatedAtBetween(runnerId, "EARNED", startTs, endTs)

        val runningTypes = setOf("TRAINING_RUN", "WORKOUT")
        val crossTypes = setOf("CROSS_TRAINING")

        val runningTxns = transactions.filter { it.activityType in runningTypes }
        val crossTxns = transactions.filter { it.activityType in crossTypes }
        val otherTxns = transactions.filter { it.activityType != null && it.activityType !in runningTypes && it.activityType !in crossTypes }

        val miles = summerTrainingAwardService.getMilesRunInWindow(
                runnerId,
                java.sql.Date(startTs.time),
                java.sql.Date(endTs.time)
        )

        return SummerConditioningBreakdownResponse(
                runnerId = runnerId,
                year = year,
                runningPoints = runningTxns.sumOf { it.pointsAmount },
                crossTrainingPoints = crossTxns.sumOf { it.pointsAmount },
                otherPoints = otherTxns.sumOf { it.pointsAmount },
                runningSessions = runningTxns.size,
                crossTrainingSessions = crossTxns.size,
                milesRun = miles
        )
    }

    fun getSummerConditioningConfig(year: String): SummerConditioningConfig {
        return summerConditioningConfigRepository.findByYear(year)
                .orElseThrow { RuntimeException("No summer conditioning config found for year $year") }
    }

    @Transactional
    fun upsertSummerConditioningConfig(request: UpsertSummerConditioningConfigRequest): SummerConditioningConfig {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val start = Date.valueOf(LocalDate.parse(request.startDate, formatter))
        val end = Date.valueOf(LocalDate.parse(request.endDate, formatter))

        val existing = summerConditioningConfigRepository.findByYear(request.year)
        return if (existing.isPresent) {
            val config = existing.get()
            config.startDate = start
            config.endDate = end
            config.pointsThreshold = request.pointsThreshold
            summerConditioningConfigRepository.save(config)
        } else {
            summerConditioningConfigRepository.save(SummerConditioningConfig(
                    year = request.year, startDate = start, endDate = end,
                    pointsThreshold = request.pointsThreshold, createdBy = request.createdBy))
        }
    }

    private data class DateWindow(val startTs: Timestamp, val endTs: Timestamp, val windowClosed: Boolean)

    private fun resolveDateWindow(config: SummerConditioningConfig): DateWindow {
        val today = LocalDate.now()
        val configEnd = config.endDate.toLocalDate()
        val effectiveEnd = if (today.isBefore(configEnd)) today else configEnd
        return DateWindow(
                startTs = Timestamp.valueOf(config.startDate.toLocalDate().atStartOfDay()),
                endTs = Timestamp.valueOf(effectiveEnd.plusDays(1).atStartOfDay()),
                windowClosed = !today.isBefore(configEnd)
        )
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