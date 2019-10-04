package com.terkula.uaxctf.statisitcs.model

import javax.persistence.*

@Entity
@Table(name = "xc_season_goals", schema = "uaxc")
class XcGoal (@Column(name = "runner_id")
              val runnerId: Int,
              val time: String,
              val season: String,
              val distance: Int){


    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @JoinColumn
    val id: Int = 0
}

