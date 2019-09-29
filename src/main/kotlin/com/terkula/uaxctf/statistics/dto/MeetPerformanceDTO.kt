package com.terkula.uaxctf.statistics.dto

import com.terkula.uaxctf.util.calculateSecondsFrom
import com.terkula.uaxctf.util.round
import com.terkula.uaxctf.util.toPaddedString
import java.sql.Date

class MeetPerformanceDTO(var meetName: String, var meetDate: Date, var time: String, var place: Int)

fun List<MeetPerformanceDTO>.getTimeDifferencesAsStrings(): MutableList<String> {
    val timeDifferences: MutableList<String> = mutableListOf()

    if (this.size >= 2) {
        for (i in 0.. this.size) {
            if (i < this.size - 1) {
                val latestMeetTime = this[i].time
                val yearPriorMeetTime = this[i + 1].time

                val latestSeconds = latestMeetTime.calculateSecondsFrom()
                val yearPriorSeconds = yearPriorMeetTime.calculateSecondsFrom()

                val rawDifference = (latestSeconds - yearPriorSeconds)
                timeDifferences.add(rawDifference.toMinuteSecondString())

            }
        }
    } else {
        timeDifferences.add("")
    }
    return timeDifferences
}


fun List<MeetPerformanceDTO>.getTimeDifferencesAsDoubles(): MutableList<Double> {
    val timeDifferences: MutableList<Double> = mutableListOf()

    if (this.size >= 2) {
        for (i in 0..this.size) {
            if (i < this.size - 1) {
                val latestMeetTime = this[i].time
                val yearPriorMeetTime = this[i + 1].time

                val latestSeconds = latestMeetTime.calculateSecondsFrom()
                val yearPriorSeconds = yearPriorMeetTime.calculateSecondsFrom()

                val rawDifference = (latestSeconds - yearPriorSeconds)
                timeDifferences.add(rawDifference)
            } else {
                timeDifferences.add(0.0)
            }


        }

    }
    return timeDifferences
}

fun Double.toMinuteSecondString(): String {
    val minutes: Int = (this / 60).toInt()
    var seconds = (this % 60).round(1)
    return if (this < 1) {
        seconds *= -1
        val secondsString = seconds.toPaddedString()
        "-" + minutes *-1 + ":" + secondsString
    } else {
        minutes.toString() + ":" + seconds.toPaddedString()
    }

}


