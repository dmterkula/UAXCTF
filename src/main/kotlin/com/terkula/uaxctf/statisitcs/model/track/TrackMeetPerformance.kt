package com.terkula.uaxctf.statisitcs.model.track

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.terkula.uaxctf.statistics.response.track.TrackMeetPerformanceResponse
import com.terkula.uaxctf.statistics.response.track.TrackSplit
import java.sql.Date
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table
import javax.sound.midi.Track

@Entity
@Table(name = "track_meet_results", schema = "uaxc")
data class TrackMeetPerformance(
         @Column(name = "meet_id")
         val meetId: String,
         val uuid: String,
         @Column(name = "runner_id")
         val runnerId: Int,
         var time: String,
         var place: Int,
         var event: String,
         @Column(name = "is_split")
         var isSplit: Boolean,
         var splits: String)
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0
}

fun TrackMeetPerformance.getLogicalEvent(): String {
    return this.event.getLogicalEvent(false)
}

fun TrackMeetPerformance.getLogicalEvent(convertToMetric: Boolean): String {
    return if (convertToMetric) this.event.getLogicalEvent(convertToMetric)
    else this.event.getLogicalEvent(false)
}

fun Int.toLogicalEvent(): String {
    return this.toString() + "m"
}

fun String.getLogicalEvent(convertToMetric: Boolean): String {

    if (convertToMetric) {
        return this.getLogicalEventMetric()
    }

    val sameGroup = listOf("60m, 60h, 100h, 110h, 300h, 2k Steeple", "5000m")

    return if (this in sameGroup) {
        this
    } else if (this == "4x100m" || this == "100m" || this == "100") {
        "100m"
    } else if (this == "4x200m" || this == "200m" || this == "200") {
        "200m"
    } else if (this == "4x400m" || this == "400m" || this == "400") {
        "400m"
    } else if (this == "4x800m" || this == "800m" || this == "800") {
        "800m"
    } else if (this == "4x1600m" || this == "1600m" || this == "1600") {
        "1600m"
    } else if (this == "4xMile" || this == "Mile" || this == "1609") {
        "Mile"
    } else if (this == "3200m" || this == "2 Mile" || this == "3218") {
        "3200m"
    }else {
        this
    }
}

fun String.getLogicalEventMetric(): String {
    val sameGroup = listOf("60m, 60h, 100h, 110h, 300h, 2k Steeple", "5000m")

    return if (this in sameGroup) {
        this
    } else if (this == "4x100m" || this == "100m" || this == "100") {
        "100m"
    } else if (this == "4x200m" || this == "200m" || this == "200") {
        "200m"
    } else if (this == "4x400m" || this == "400m" || this == "400") {
        "400m"
    } else if (this == "4x800m" || this == "800m" || this == "800") {
        "800m"
    } else if (this == "4x1600m" || this == "1600m" || this == "1600" || this == "4xMile" || this == "Mile" || this == "1609") {
        "1600m"
    }  else if (this == "3200m" || this == "2 Mile" || this == "3218") {
        "3200m"
    } else {
        this
    }
}

fun String.getMilesDistanceFromLogicalEvent(): Double {

    if (this.equals("2k Steeple", ignoreCase = true)) {
        return 2000.0/1609
    }

    if (this.equals("Mile", ignoreCase = true)) {
        return 1.0
    }

    return this.filter{ it.isDigit() }.toDouble() / 1609
}

//fun List<TrackMeetPerformance>.toTrackMeetPerformanceDTO(meet: TrackMeet, runner: Runner): List<TrackMeetPerformanceDTO> {
//    return this.map { TrackMeetPerformanceDTO(meet: meet }
//}

fun TrackMeetPerformance.eventDistance(): Int {

    val acceptedDistances = listOf("100", "200", "400", "800", "1600", "1609", "3200")

    return acceptedDistances.firstOrNull { this.event.contains(it) }?.toInt() ?: 0

}

fun TrackMeetPerformance.toTrackMeetPerformanceResponse(meetName: String, meetDate: Date): TrackMeetPerformanceResponse {

    val splits: List<TrackSplit> = ObjectMapper()
            .registerKotlinModule()
            .readValue(this.splits)

    return TrackMeetPerformanceResponse(this.meetId, meetName, meetDate, this.uuid, this.runnerId, this.time, this.place, this.event, this.isSplit, splits)
}

fun List<TrackMeetPerformance>.toTrackMeetPerformancesResponses(meetName: String, meetDate: Date): List<TrackMeetPerformanceResponse> {
    return this.map { it.toTrackMeetPerformanceResponse(meetName, meetDate) }
}