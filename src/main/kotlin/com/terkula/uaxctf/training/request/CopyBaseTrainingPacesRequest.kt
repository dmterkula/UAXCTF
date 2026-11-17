package com.terkula.uaxctf.training.request

/**
 * Copies base training paces from one season to another, e.g. XC 2026 5000m -> track 2027 5000m.
 *
 * adjustmentType:
 *  - NONE: copy the pace as-is
 *  - PERCENT: slow the pace by adjustmentValue percent (3.0 = 3% slower)
 *  - SECONDS_PER_MILE: slow the pace by adjustmentValue seconds per mile (10.0 = +10 s/mi)
 *  - TOTAL_SECONDS: add adjustmentValue seconds to the whole race time (30.0 = 18:50 -> 19:20)
 * Negative values speed the pace up.
 *
 * runnerIds: runners to copy. When empty and dryRun = true, previews every runner with a source pace.
 * dryRun: true returns the preview only and writes nothing.
 * overwriteExisting: false skips runners who already have a target pace. true adds a newer record
 *  (older records are kept for history; the newest record wins).
 */
class CopyBaseTrainingPacesRequest(
    val fromSeason: String = "xc",
    val fromYear: String,
    val fromEvent: String = "5000m",
    val toSeason: String = "track",
    val toYear: String,
    val toEvent: String = "5000m",
    val runnerIds: List<Int> = emptyList(),
    val adjustmentType: String = "NONE",
    val adjustmentValue: Double = 0.0,
    val dryRun: Boolean = true,
    val overwriteExisting: Boolean = false
)
