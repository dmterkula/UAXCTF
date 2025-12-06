package com.terkula.uaxctf.statistics.request

data class ValidatePointsRequest(
        val runnerId: Int,
        val featureType: String
)

data class SpendPointsRequest(
        val runnerId: Int,
        val featureType: String,
        val description: String? = null
)

data class RefundPointsRequest(
        val transactionId: Long
)

data class UpdatePointsConfigRequest(
        val configKey: String,
        val configValue: Int,
        val updatedBy: String? = null
)

data class EarnPointsRequest(
        val runnerId: Int,
        val activityType: String,
        val activityUuid: String? = null,
        val season: String? = null,
        val year: String? = null,
        val description: String? = null
)