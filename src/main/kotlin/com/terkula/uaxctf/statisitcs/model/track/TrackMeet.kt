package com.terkula.uaxctf.statisitcs.model.track

import com.terkula.uaxctf.statisitcs.model.Venue
import java.sql.Date
import javax.persistence.*

@Entity
@Table(name = "track_meets", schema = "uaxc")
data class TrackMeet(
              var name: String,
              var uuid: String,
              var date: Date,
              var icon: String,
              var team: String,
              @Column(name = "venue_id")
              var venueId: Int? = null
)
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JoinColumn
    val id: Int = 0

    @Transient
    var venue: Venue? = null
}