package com.terkula.uaxctf.statistics.response

import com.terkula.uaxctf.statisitcs.model.PointTransaction
import com.terkula.uaxctf.statisitcs.model.Runner
import java.sql.Timestamp

data class PointBalanceResponse(
        val currentPoints: Int,
        val lifetimePointsEarned: Int,
        val lastUpdated: Timestamp?
)

data class ValidatePointsResponse(
        val hasEnoughPoints: Boolean,
        val currentBalance: Int,
        val pointsRequired: Int,
        val shortfall: Int
)

data class SpendPointsResponse(
        val success: Boolean,
        val newBalance: Int,
        val pointsSpent: Int,
        val transaction: PointTransaction? = null,
        val errorMessage: String? = null
)

data class RefundPointsResponse(
        val success: Boolean,
        val newBalance: Int,
        val pointsRefunded: Int,
        val errorMessage: String? = null
)

data class PointsConfigResponse(
        val config: Map<String, Int>
)

data class EarnPointsResponse(
        val success: Boolean,
        val pointsEarned: Int,
        val newBalance: Int,
        val transaction: PointTransaction? = null
)

data class RunnerSummerConditioningDTO(
        val runner: Runner,
        val pointsEarned: Int,
        val pointsThreshold: Int
)

data class SummerConditioningResponse(
        val year: String,
        val startDate: String,
        val endDate: String,
        val pointsThreshold: Int,
        val windowClosed: Boolean,
        val runners: List<RunnerSummerConditioningDTO>
)

data class SummerConditioningConfigDTO(
        val year: String,
        val startDate: String,
        val endDate: String,
        val pointsThreshold: Int
)

data class SummerConditioningBreakdownResponse(
        val runnerId: Int,
        val year: String,
        val runningPoints: Int,
        val crossTrainingPoints: Int,
        val otherPoints: Int,
        val runningSessions: Int,
        val crossTrainingSessions: Int,
        val milesRun: Double
)