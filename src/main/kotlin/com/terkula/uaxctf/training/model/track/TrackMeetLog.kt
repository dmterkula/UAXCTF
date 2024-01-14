package com.terkula.uaxctf.training.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.terkula.uaxctf.statisitcs.model.track.getLogicalEvent
import com.terkula.uaxctf.statisitcs.model.track.getMilesDistanceFromLogicalEvent
import com.terkula.uaxctf.util.calculateSecondsFrom
import com.terkula.uaxctf.util.toMinuteSecondString
import javax.persistence.*

@Entity
@Table(name = "track_meet_log", schema = "uaxc")
@JsonInclude(value = JsonInclude.Include.NON_NULL)
class TrackMeetLog( @Column(name = "meet_uuid")
               var meetId: String,
               @Column(name = "runner_id")
               var runnerId: Int,
               var time: String,
               @Column(name = "warm_up_distance")
               var warmUpDistance: Double?,
               @Column(name = "warm_up_time")
               var warmUpTime: String?,
               @Column(name = "warm_up_pace")
               var warmUpPace: String?,
               @Column(name = "cool_down_distance")
               var coolDownDistance: Double?,
               @Column(name = "cool_down_time")
               var coolDownTime: String?,
               @Column(name = "cool_down_pace")
               var coolDownPace: String?,
               var notes: String?,
               @Column(name = "coach_notes")
               var coachNotes: String?,
               @Column(name = "season")
               var season: String,
               @Column(name = "event")
               var event: String
) {

    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JoinColumn
    val id: Int = 0

    fun getTotalDistance(): Double {

        var totalDistance = event.getLogicalEvent(false).getMilesDistanceFromLogicalEvent()

        if (warmUpDistance != null) {
            totalDistance += warmUpDistance!!
        }

        if (coolDownDistance != null) {
            totalDistance += coolDownDistance!!
        }

        return totalDistance
    }

    fun getTotalTimeSeconds(): Double {

        var totalTime = 0.0

        if (time.isNotEmpty()) {
            totalTime += time.calculateSecondsFrom()
        }

        if (warmUpTime != null && warmUpTime!!.isNotEmpty()) {
            totalTime += warmUpTime!!.calculateSecondsFrom()
        }

        if (coolDownTime != null && coolDownTime!!.isNotEmpty()) {
            totalTime += coolDownTime!!.calculateSecondsFrom()
        }

        return totalTime
    }

    fun getTotalAveragePace(): String {
        return (getTotalTimeSeconds() / getTotalDistance()).toMinuteSecondString()
    }

    fun getRaceOnlyDistance(): Double {
        return event.getLogicalEvent(false).getMilesDistanceFromLogicalEvent()
    }

}