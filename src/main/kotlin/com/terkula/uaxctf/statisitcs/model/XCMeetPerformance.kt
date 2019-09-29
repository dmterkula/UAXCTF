package com.terkula.uaxctf.statisitcs.model

import com.terkula.uaxctf.statistics.dto.MeetPerformanceDTO
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
         var place: Int)

{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0
}

fun List<XCMeetPerformance>.toMeetPerformanceDTO(meetMap: Map<Int, Meet>): List<MeetPerformanceDTO> {
    return this.map { MeetPerformanceDTO(meetMap[it.meetId]!!.name, meetMap[it.meetId]!!.date, it.time, it.place) }
}