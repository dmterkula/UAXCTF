package com.terkula.uaxctf.statistics.response

import com.terkula.uaxctf.statisitcs.model.Runner
import java.sql.Date

data class InsightItem(
    val type: String,        // READINESS_RED, READINESS_YELLOW, LOGGING_STRONG, MILEAGE_LEADER, DAYS_SINCE_PR, DAYS_SINCE_SB
    val severity: String,    // HIGH, MEDIUM, POSITIVE
    val runner: Runner,
    val headline: String,
    val detail: String,
    val deepLinkKey: String, // "readiness", "training", "meets"
    val generatedDate: Date
)

data class CoachInsightsResponse(val insights: List<InsightItem>)
