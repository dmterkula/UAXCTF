package com.terkula.uaxctf.statistics.response

import com.terkula.uaxctf.statisitcs.model.Runner

data class ReadinessSignal(
    val type: String,        // PAIN, EFFORT_HIGH, LOGGING_GAP, LOAD_SPIKE
    val severity: String,    // YELLOW, RED
    val detail: String,
    val sortValue: Double    // numeric magnitude for within-group sorting (higher = more offending)
    // PAIN: pain level · EFFORT_HIGH: overage above ceiling · LOAD_SPIKE: WoW% · LOGGING_GAP: sessions missing
)

data class RunnerReadinessResponse(
    val runner: Runner,
    val status: String,             // GREEN, YELLOW, RED
    val signals: List<ReadinessSignal>,
    val weeklyLoad: Double,         // current week sRPE load
    val loadWowPct: Double?,        // week-over-week % change (null if no prior week data)
    val ewmaLoad: Double,           // EWMA of weekly load (smoothed)
    val loggedSessions7d: Int,      // activity count in trailing 7 days
    val daysSinceLastLog: Int,      // days since most recent logged activity
    val dataComplete: Boolean,      // false if logging gap makes mileage untrustworthy
    val weeklyLoads: List<Double>   // sRPE load per week, index 0 = this week, index 4 = oldest
)

data class TeamReadinessResponse(val runners: List<RunnerReadinessResponse>)
