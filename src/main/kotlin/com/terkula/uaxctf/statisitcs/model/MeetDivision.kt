package com.terkula.uaxctf.statisitcs.model

import javax.persistence.*

/**
 * A single race within a meet (Varsity, JV, Open, etc.) — XC meets commonly run several
 * of these back to back, each with its own start time. [sequence] is the running order
 * (1 = first race of the day) and is the primary signal used to match scraped results to
 * a pre-configured division, since division *names* vary by timing site and rarely match
 * what a coach typed in ahead of time — but race order is reliable.
 */
@Entity
@Table(name = "meet_divisions", schema = "uaxc")
data class MeetDivision(
    @Column(name = "meet_id")
    var meetId: Int,
    var name: String,
    var sequence: Int,
    @Column(name = "start_time")
    var startTime: java.sql.Time? = null,
    @Column(name = "distance_meters")
    var distanceMeters: Int? = null
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JoinColumn
    val id: Int = 0
}
