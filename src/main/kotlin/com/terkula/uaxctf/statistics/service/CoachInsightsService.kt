package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statistics.request.SortingMethodContainer
import com.terkula.uaxctf.statistics.response.CoachInsightsResponse
import com.terkula.uaxctf.statistics.response.InsightItem
import org.springframework.stereotype.Service
import java.sql.Date
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private object InsightsDefaults {
    const val LOGGING_STRONG_THRESHOLD = 5.0
    const val TOP_LOAD_COUNT           = 3.0
    const val SB_STALE_DAYS            = 21.0
}

@Service
class CoachInsightsService(
    val readinessService: ReadinessService,
    val personalRecordService: PersonalRecordService,
    val seasonBestService: SeasonBestService,
    val configService: TrainingLoadConfigService
) {

    fun getInsights(season: String, referenceDate: Date, team: String?): CoachInsightsResponse {
        val items = mutableListOf<InsightItem>()
        val refLocal = referenceDate.toLocalDate()
        val yearStart = Date.valueOf(LocalDate.of(refLocal.year, 1, 1))

        val loggingThreshold = configService.get("insights.logging_strong_threshold", team, InsightsDefaults.LOGGING_STRONG_THRESHOLD).toInt()
        val topLoadCount     = configService.get("insights.top_load_count",            team, InsightsDefaults.TOP_LOAD_COUNT).toInt()
        val sbStaleDays      = configService.get("insights.sb_stale_days",             team, InsightsDefaults.SB_STALE_DAYS).toInt()

        // --- 1. Readiness items ---
        val readiness = readinessService.getTeamReadiness(season, referenceDate, team)
        readiness.runners.forEach { r ->
            when (r.status) {
                "RED" -> items.add(InsightItem(
                    type = "READINESS_RED", severity = "HIGH", runner = r.runner,
                    headline = "${r.runner.name} — readiness alert",
                    detail = r.signals.firstOrNull { it.severity == "RED" }?.detail
                        ?: r.signals.firstOrNull()?.detail ?: "Multiple signals flagged",
                    deepLinkKey = "readiness", generatedDate = referenceDate
                ))
                "YELLOW" -> items.add(InsightItem(
                    type = "READINESS_YELLOW", severity = "MEDIUM", runner = r.runner,
                    headline = "${r.runner.name} — check in",
                    detail = r.signals.firstOrNull()?.detail ?: "Signal flagged",
                    deepLinkKey = "readiness", generatedDate = referenceDate
                ))
            }
        }

        // Active runner IDs from readiness — already filtered to the correct season roster
        val activeRunnerIds = readiness.runners.map { it.runner.id }.toSet()

        // --- 2. Days since last PR (always-on tracker, always POSITIVE) ---
        try {
            personalRecordService.getAllPRs("2000", "", SortingMethodContainer.NOSORT, false)
                .filter { activeRunnerIds.contains(it.runner.id) }
                .forEach { prdto ->
                    val recentPR = prdto.pr.maxByOrNull { it.meetDate } ?: return@forEach
                    val daysSince = ChronoUnit.DAYS.between(recentPR.meetDate.toLocalDate(), refLocal).toInt()
                    items.add(InsightItem(
                        type = "DAYS_SINCE_PR", severity = "POSITIVE", runner = prdto.runner,
                        headline = "${prdto.runner.name} — last PR: ${recentPR.time}",
                        detail = "${recentPR.meetName} · ${daysSince}d ago",
                        deepLinkKey = "meets", generatedDate = referenceDate
                    ))
                }
        } catch (_: Exception) {}

        // --- 3. Days since last SB — MEDIUM if stale, POSITIVE otherwise; no-op in summer ---
        try {
            seasonBestService.getAllSeasonBests(yearStart, referenceDate, false)
                .filter { activeRunnerIds.contains(it.runner.id) }
                .forEach { sbdto ->
                    val sb = sbdto.seasonBest.firstOrNull() ?: return@forEach
                    val daysSince = ChronoUnit.DAYS.between(sb.meetDate.toLocalDate(), refLocal).toInt()
                    val stale = daysSince > sbStaleDays
                    items.add(InsightItem(
                        type = "DAYS_SINCE_SB",
                        severity = if (stale) "MEDIUM" else "POSITIVE",
                        runner = sbdto.runner,
                        headline = if (stale) "${sbdto.runner.name} — no SB in ${daysSince}d"
                                   else       "${sbdto.runner.name} — current SB: ${sb.time}",
                        detail = "${sb.time} at ${sb.meetName} · ${daysSince}d ago${if (stale) " — potential fatigue" else ""}",
                        deepLinkKey = "meets", generatedDate = referenceDate
                    ))
                }
        } catch (_: Exception) {}

        // --- 4. Positive: strong logging this week ---
        readiness.runners
            .filter { it.loggedSessions7d >= loggingThreshold && it.status == "GREEN" }
            .forEach { r ->
                items.add(InsightItem(
                    type = "LOGGING_STRONG", severity = "POSITIVE", runner = r.runner,
                    headline = "${r.runner.name} — consistent week",
                    detail = "${r.loggedSessions7d} sessions logged this week",
                    deepLinkKey = "training", generatedDate = referenceDate
                ))
            }

        // --- 5. Positive: top load leaders this week ---
        readiness.runners
            .filter { it.weeklyLoad > 0 && it.dataComplete }
            .sortedByDescending { it.weeklyLoad }
            .take(topLoadCount)
            .forEach { r ->
                val wowStr = r.loadWowPct?.let { pct ->
                    val sign = if (pct >= 0) "+" else ""; " ($sign${pct.toInt()}% WoW)"
                } ?: ""
                items.add(InsightItem(
                    type = "MILEAGE_LEADER", severity = "POSITIVE", runner = r.runner,
                    headline = "${r.runner.name} — top load this week",
                    detail = "sRPE: ${r.weeklyLoad.toInt()} · ${r.loggedSessions7d} sessions$wowStr",
                    deepLinkKey = "training", generatedDate = referenceDate
                ))
            }

        return CoachInsightsResponse(
            items.sortedWith(
                compareByDescending<InsightItem> { severityOrder(it.severity) }
                    .thenByDescending { it.generatedDate }
            )
        )
    }

    private fun severityOrder(s: String) = when (s) {
        "HIGH" -> 3; "MEDIUM" -> 2; "POSITIVE" -> 1; else -> 0
    }
}
