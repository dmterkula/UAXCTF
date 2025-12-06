package com.terkula.uaxctf.statistics.response

import com.terkula.uaxctf.statisitcs.model.PointTransaction
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