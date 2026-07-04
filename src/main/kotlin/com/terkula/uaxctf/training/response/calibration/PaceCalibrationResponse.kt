package com.terkula.uaxctf.training.response.calibration

import com.terkula.uaxctf.statisitcs.model.Runner

data class PaceCalibrationResponse(
    val runners: List<RunnerPaceCalibration>
)

data class RunnerPaceCalibration(
    val runner: Runner,
    val currentBaseTime: String,
    val baseSource: String,          // "RACE_DERIVED" | "COACH_ESTIMATE"
    val confidence: Int,             // 0–100
    val confidenceLabel: String,     // "LOW" | "MEDIUM" | "HIGH"
    val verdict: String,             // "BASE_OFF" | "ZONE_OFF" | "DIALED_IN" | "INSUFFICIENT_DATA"
    val suggestion: CalibrationSuggestion?,
    val zones: List<ZoneCalibration>
)

data class CalibrationSuggestion(
    val knob: String,            // "BASE" | "ZONE_PERCENT"
    val currentValue: String,
    val suggestedValue: String,
    val deltaHuman: String,
    val applicable: Boolean      // false = advisory only (workout zones — no write path yet)
)

data class ZoneCalibration(
    val paceType: String,        // e.g. "recovery", "tempo", "threshold 90"
    val zoneClass: String,       // "WORKOUT" | "RUN"
    val n: Int,
    val meanRelDev: Double,      // signed fraction: negative = faster, positive = slower
    val expectedPace: String,
    val observedAvgPace: String,
    val inCount: Int,
    val fastCount: Int,
    val slowCount: Int,
    val confidence: Int,
    val sufficientData: Boolean
)
