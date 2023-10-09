package com.terkula.uaxctf.statisitcs.model.track

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.terkula.uaxctf.statistics.response.track.TrackMeetPerformanceResponse
import com.terkula.uaxctf.statistics.response.track.TrackSplit
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

//fun List<TrackMeetPerformance>.toTrackMeetPerformanceDTO(meet: TrackMeet, runner: Runner): List<TrackMeetPerformanceDTO> {
//    return this.map { TrackMeetPerformanceDTO(meet: meet }
//}

fun TrackMeetPerformance.eventDistance(): Int {

    val acceptedDistances = listOf("100", "200", "400", "800", "1600", "1609", "3200")

    return acceptedDistances.firstOrNull { this.event.contains(it) }?.toInt() ?: 0

}

fun TrackMeetPerformance.toTrackMeetPerformanceResponse(): TrackMeetPerformanceResponse {

    val splits: List<TrackSplit> = ObjectMapper().readValue(this.splits)

    return TrackMeetPerformanceResponse(this.meetId, this.uuid, this.runnerId, this.time, this.place, this.event, this.isSplit, splits)
}

fun List<TrackMeetPerformance>.toTrackMeetPerformancesResponses(): List<TrackMeetPerformanceResponse> {
    return this.map { it.toTrackMeetPerformanceResponse() }
}