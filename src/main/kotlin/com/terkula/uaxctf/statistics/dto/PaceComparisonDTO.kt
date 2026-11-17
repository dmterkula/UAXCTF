package com.terkula.uaxctf.statistics.dto

import java.sql.Date

class PaceSourceBreakdownDTO(
    val sb: Int,
    val pr: Int,
    val timeTrial: Int,
    val none: Int
)

class MileSplitStatDTO(
    val mile: Int,
    val meanPercentOfPace: Double,
    val stdDev: Double,
    val n: Int
)

class SplitDifferenceSecondsDTO(
    val firstToSecond: Double,
    val secondToThird: Double,
    val spread: Double
)

class MileSplitSignificanceDTO(
    val mile: Int,
    val pValue: Double,
    val significant: Boolean
)

class VsBaseComparisonDTO(
    val splits: List<MileSplitSignificanceDTO>
)

class MeetPaceComparisonDTO(
    val season: String,
    val meetName: String,
    val date: Date,
    val runnerCount: Int,
    val paceSourceBreakdown: PaceSourceBreakdownDTO,
    val splits: List<MileSplitStatDTO>,
    val avgPercentOfPaceAllMiles: Double,
    val avgSplitDifferenceSeconds: SplitDifferenceSecondsDTO,
    val isBase: Boolean,
    val vsBase: VsBaseComparisonDTO?
)

class MeetPaceRankingDTO(
    val season: String,
    val meetName: String,
    val rank: Int,
    val avgPercentOfPaceAllMiles: Double
)

class PaceComparisonResponse(
    val basePace: String,
    val meets: List<MeetPaceComparisonDTO>,
    val ranking: List<MeetPaceRankingDTO>,
    // False whenever heat adjustment was requested but weather couldn't be resolved for every meet
    // in the comparison -- in that case ALL meets fall back to raw times rather than silently mixing
    // adjusted and unadjusted data, which would make the significance tests meaningless.
    val heatAdjustmentApplied: Boolean = false,
    // Same all-or-nothing semantics as heatAdjustmentApplied, but for course-grade normalization --
    // independent flag since a comparison can have full weather coverage but partial course-profile coverage, or vice versa.
    val gradeAdjustmentApplied: Boolean = false
)

class WeekPaceComparisonDTO(
    val week: Int,
    val meetNames: List<String>,
    val startDate: Date,
    val runnerCount: Int,
    val paceSourceBreakdown: PaceSourceBreakdownDTO,
    val splits: List<MileSplitStatDTO>,
    val avgPercentOfPaceAllMiles: Double,
    val vsBase: VsBaseComparisonDTO?
)

class SeasonPaceComparisonDTO(
    val season: String,
    val meetCount: Int,
    val runnerCount: Int,
    val paceSourceBreakdown: PaceSourceBreakdownDTO,
    val splits: List<MileSplitStatDTO>,
    val avgPercentOfPaceAllMiles: Double,
    val isBase: Boolean,
    val vsBase: VsBaseComparisonDTO?,
    val weeks: List<WeekPaceComparisonDTO>
)

class SeasonPaceRankingDTO(
    val season: String,
    val rank: Int,
    val avgPercentOfPaceAllMiles: Double
)

class SeasonPaceComparisonResponse(
    val basePace: String,
    val seasons: List<SeasonPaceComparisonDTO>,
    val ranking: List<SeasonPaceRankingDTO>,
    val heatAdjustmentApplied: Boolean = false,
    val gradeAdjustmentApplied: Boolean = false
)