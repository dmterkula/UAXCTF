package com.terkula.uaxctf.statisitcs.model.track

import java.sql.Date
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.Table

@Entity
@Table(name = "track_meets", schema = "uaxc")
data class TrackMeet(
              var name: String,
              var uuid: String,
              var date: Date,
              var icon: String
)
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JoinColumn
    val id: Int = 0
}