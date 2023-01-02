package com.terkula.uaxctf.statistics.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.terkula.uaxctf.util.calculateSecondsFrom
import com.terkula.uaxctf.util.round
import com.terkula.uaxctf.util.toMinuteSecondString
import java.sql.Date

@JsonInclude(JsonInclude.Include.NON_NULL)
class MeetPerformanceDTO(var meetName: String, var meetDate: Date, var time: String, var place: Int, var adjustedTimeAmount: Double?,
    var passesSecondMile: Int, var passesLastMile: Int, var skullsEarned: Int)

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
fun MeetPerformanceDTO.adjustForDistance(distance: Int): MeetPerformanceDTO {
    return if (distance != 5000) {
        val ratio = distance / 5000.0

        val newTime = (this.time.calculateSecondsFrom() / ratio)
        val newTimeString = newTime.toMinuteSecondString()
        val timeDifference = newTime - this.time.calculateSecondsFrom()

        MeetPerformanceDTO(this.meetName, this.meetDate, newTimeString, this.place, timeDifference.round(2),
        this.passesSecondMile, this.passesLastMile, this.skullsEarned)
    } else {
        this
    }
}



