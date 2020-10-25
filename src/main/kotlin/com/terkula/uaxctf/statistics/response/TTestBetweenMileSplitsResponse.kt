package com.terkula.uaxctf.statistics.response

import com.terkula.uaxctf.statistics.dto.StatisticalComparisonDTO
import com.terkula.uaxctf.statistics.dto.TStatDTO

class TTestBetweenMileSplitsResponse (
        val distributionSummaryBaseYear: List<StatisticalComparisonDTO>,
        val distributionSummaryComparisonYear: List<StatisticalComparisonDTO>,
        val tTestResults: List<TStatDTO>) {
}