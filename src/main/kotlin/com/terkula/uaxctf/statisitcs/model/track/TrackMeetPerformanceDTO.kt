package com.terkula.uaxctf.statisitcs.model.track

import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.util.calculateSecondsFrom
import com.terkula.uaxctf.util.toMinuteSecondString
import java.sql.Date

class TrackMeetPerformanceDTO(
        val meet: TrackMeet,
        val runner: Runner,
        val results: List<TrackMeetPerformance>
)

//fun List<TrackMeetPerformanceDTO>.getTimeDifferencesAsStrings(): MutableList<String> {
//    val timeDifferences: MutableList<String> = mutableListOf()
//
//    if (this.size >= 2) {
//        for (i in 0.. this.size) {
//            if (i < this.size - 1) {
//                val latestMeetTime = this[i].time
//                val yearPriorMeetTime = this[i + 1].time
//
//                val latestSeconds = latestMeetTime.calculateSecondsFrom()
//                val yearPriorSeconds = yearPriorMeetTime.calculateSecondsFrom()
//
//                val rawDifference = (latestSeconds - yearPriorSeconds)
//                timeDifferences.add(rawDifference.toMinuteSecondString())
//
//            }
//        }
//    } else {
//        timeDifferences.add("")
//    }
//    return timeDifferences
//}


//fun List<TrackMeetPerformanceDTO>.getTimeDifferencesAsDoubles(): MutableList<Double> {
//    val timeDifferences: MutableList<Double> = mutableListOf()
//
//    if (this.size >= 2) {
//        for (i in 0..this.size) {
//            if (i < this.size - 1) {
//                val latestMeetTime = this[i].time
//                val yearPriorMeetTime = this[i + 1].time
//
//                val latestSeconds = latestMeetTime.calculateSecondsFrom()
//                val yearPriorSeconds = yearPriorMeetTime.calculateSecondsFrom()
//
//                val rawDifference = (latestSeconds - yearPriorSeconds)
//                timeDifferences.add(rawDifference)
//            } else {
//                timeDifferences.add(0.0)
//            }
//
//
//        }
//
//    }
//    return timeDifferences
//}


