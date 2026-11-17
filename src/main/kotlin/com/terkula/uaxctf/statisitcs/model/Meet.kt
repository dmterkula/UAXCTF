package com.terkula.uaxctf.statisitcs.model

import java.sql.Date
import javax.persistence.*

@Entity
@Table(name = "meets", schema = "uaxc")
data class Meet(
      var name: String,
      var date: Date,
      var uuid: String,
      var icon: String,
      var team: String,
      @Column(name = "venue_id")
      var venueId: Int? = null,
      @Column(name = "start_time")
      var startTime: java.sql.Time? = null,
      @Column(name = "course_profile_id")
      var courseProfileId: Int? = null,
      @Column(name = "excluded_from_stats")
      var excludedFromStats: Boolean = false
)
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JoinColumn
    val id: Int = 0

    @Transient
    var venue: Venue? = null
}

/**
 * Drops meets flagged [Meet.excludedFromStats] -- races whose times aren't comparable to a normal 5k
 * (e.g. a course shortened to 4k for flooding). Apply this wherever a meet list feeds a time-based
 * comparison across meets: PRs, season bests, progressions, improvement rate, historical meet
 * comparison. The results themselves are untouched and still show on the meet's own page, and
 * non-time stats (skulls, passes, place) deliberately still count -- those are just as real at any
 * distance.
 */
fun Iterable<Meet>.statsEligible(): List<Meet> = this.filter { !it.excludedFromStats }