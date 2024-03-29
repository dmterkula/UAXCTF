package com.terkula.uaxctf.statisitcs.model

import com.terkula.uaxctf.statistics.dto.MeetPerformanceDTO
import com.terkula.uaxctf.statistics.dto.adjustForDistance
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "xc_meet_performances", schema = "uaxc")
data class XCMeetPerformance(
         @Column(name = "runner_id")
         var runnerId: Int,
         @Column(name = "meet_id")
         var meetId: Int,
         var time: String,
         var place: Int,
         @Column(name = "passes_second_mile")
         var passesSecondMile: Int,
         @Column(name = "passes_last_mile")
         var passesLastMile: Int,
         @Column(name = "skulls_earned")
         var skullsEarned: Int
)

{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0
}

fun List<XCMeetPerformance>.toMeetPerformanceDTO(meetMap: Map<Int, Meet>): List<MeetPerformanceDTO> {
    return this.map { MeetPerformanceDTO(meetMap[it.meetId]!!.name, meetMap[it.meetId]!!.date, it.time, it.place, null,
    it.passesSecondMile, it.passesLastMile, it.skullsEarned) }
}

fun XCMeetPerformance.toMeetPerformanceDTO(meet: Meet): MeetPerformanceDTO {
    return MeetPerformanceDTO(meet.name, meet.date, this.time, this.place, null,
            this.passesSecondMile, this.passesLastMile, this.skullsEarned)
}