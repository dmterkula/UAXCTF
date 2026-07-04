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