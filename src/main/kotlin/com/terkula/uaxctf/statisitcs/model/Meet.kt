package com.terkula.uaxctf.statisitcs.model

import java.sql.Date
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.Table

@Entity
@Table(name = "meets", schema = "uaxc")
data class Meet(
      var name: String,
      var date: Date,
      var uuid: String,
      var icon: String,
      var team: String
)
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JoinColumn
    val id: Int = 0
}