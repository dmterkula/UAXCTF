package com.terkula.uaxctf.statistics.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.terkula.uaxctf.util.formatedTo
import com.terkula.uaxctf.util.round
import org.nield.kotlinstatistics.percentile
import org.nield.kotlinstatistics.percentileBy
import org.nield.kotlinstatistics.standardDeviation

class StatisticalComparisonDTO(

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
        val meanDifference: String,
        val samplesSize: Int
)
{

        companion object {
                fun from(label: String, data: List<Double>, format: String, roundTo: Int): StatisticalComparisonDTO {

                        return StatisticalComparisonDTO(
                                label,
                                data.standardDeviation().round(roundTo),
                                data.percentile(10.0).round(roundTo).formatedTo(format),
                                data.percentile(25.0).round(roundTo).formatedTo(format),
                                data.percentile(75.0).round(roundTo).formatedTo(format),
                                data.percentile(90.0).round(roundTo).formatedTo(format),
                                data.average().round(roundTo).formatedTo(format),
                                data.size
                        )
                }
        }

}

class MeetSplitStatisticalComparisonDTO(
        val meet: String,
        val meanRank: Int,
        val statisticalComparison: StatisticalComparisonDTO
)