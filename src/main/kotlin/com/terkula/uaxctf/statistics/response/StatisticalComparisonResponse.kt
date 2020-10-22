package com.terkula.uaxctf.statistics.response

import com.fasterxml.jackson.annotation.JsonProperty

class StatisticalComparisonResponse(

        val label: String,
        val standardDeviation: Double,
        @JsonProperty("10thPercentile")
        val percentile10: String,
        @JsonProperty("25thPercentile")
        val percentile25: String,
        @JsonProperty("75thPercentile")
        val percentile75: String,
        @JsonProperty("90thPercentile")
        val percentile90: String,
        val meanDifference: String
) {
}