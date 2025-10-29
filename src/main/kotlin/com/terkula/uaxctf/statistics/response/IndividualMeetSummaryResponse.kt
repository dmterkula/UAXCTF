package com.terkula.uaxctf.statistics.response

import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.statistics.dto.*

class IndividualMeetSummaryResponse(
        var result: RunnerMeetPerformanceDTO,
        var runner: Runner,
        var isPR: Boolean,
        var isSB: Boolean,
        val historicalMeetRank: Int,
        val secondMileSlowDownRank: Int,
        val secondMileSlowDownPercentile: Double,
        val thirdMileSlowDownRank: Int,
        val thirdMileSlowDownPercentile: Double,
        val splitSpreadRank: Int,
        val splitSpreadPercentile: Double,
        val passesSecondMileVsFinishPosition: Double,
        val passesThirdMileVsFinishPosition: Double,
        val passesSecondMileVsFinishPositionRank: Int,
        val passesThirdMileVsFinishPositionRank: Int,
        val passesSecondMileVsFinishPositionPercentile: Double,
        val passesThirdMileVsFinishPositionPercentile: Double,
        val totalPassesRank: Int,
        val totalPassesPercentile: Double,
        val totalPassesVsFinishPosition: Double,
        val sbSplitAnalysisAverages: List<SplitComparisonAverageDTO>,
        val paceAnalysisToSB: RunnersMeetSplitsComparisonPaceDTO?,
        val baseTrainingSplitAnalysisAverages: List<SplitComparisonAverageDTO>,
        val paceAnalysisToBaseTraining: RunnersMeetSplitsComparisonPaceDTO?,
        val mileOneCareerRank: Int,
        val mileTwoCareerRank: Int,
        val mileThreeCareerRank: Int,
        val mileOneSeasonRank: Int,
        val mileTwoSeasonRank: Int,
        val mileThreeSeasonRank: Int
) {
}