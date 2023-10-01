package com.terkula.uaxctf.statisitcs.model.track

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

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