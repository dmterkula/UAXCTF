package com.terkula.uaxctf.statistics.dto

import com.terkula.uaxctf.statisitcs.model.Meet
import com.terkula.uaxctf.statisitcs.model.MeetMileSplit
import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.util.calculateSecondsFrom
import com.terkula.uaxctf.util.round
import com.terkula.uaxctf.util.toMinuteSecondString

class MeetSplitsDTO(val mileOne: String, val mileTwo: String, val mileThree: String) {

    var average: String = (((mileOne.calculateSecondsFrom() + mileTwo.calculateSecondsFrom()
    + mileThree.calculateSecondsFrom())/ 3).round(2)).toMinuteSecondString()

    companion object {
        const val CONSISTENT_SPREAD = 20
    }

    fun isConsistentRace(): Boolean {
        val splits: List<Double> = listOf(mileOne.calculateSecondsFrom(),
                mileTwo.calculateSecondsFrom(),
                mileThree.calculateSecondsFrom())

        val max = splits.maxOrNull()
        val min = splits.minOrNull()

        return if (max != null && min != null) {
            val spread = max!! - min!!

            spread <= CONSISTENT_SPREAD
        } else {
            false
        }
    }

}

class MeetSplitToComparisonPace(val mileNumber: Int, val time: String, val percentOfComparison: Double,
                                var rankOnTeam: Int, var percentileOfTeam: Double, var standardDeviationsFromMean: Double) {

}

class MeetSplitsToComparisonPaceDTO(val runnerName: String, val splits: List<MeetSplitToComparisonPace>)

class RunnersMeetSplitsComparisonPaceDTO(val meet: Meet, val splits: List<MeetSplitsToComparisonPaceDTO>)

class RunnerMeetSplitsComparisonWithAveragesDTO(val meetSplits: List<RunnersMeetSplitsComparisonPaceDTO>, val splitAverages: List<SplitComparisonAverageDTO>)

class SplitComparisonAverageDTO(val mileNumber: Int, val averageRank: Double, val averagePercentile: Double, val averageComparisonPace: Double)

class RunnerSplitComparisonAveragesDTO(val runner: String, val splitAverages: List<SplitComparisonAverageDTO>)

fun MeetSplitsDTO.toMeetSplit() : MeetMileSplit {

    return MeetMileSplit(0, 0, this.mileOne, this.mileTwo, this.mileThree)

}