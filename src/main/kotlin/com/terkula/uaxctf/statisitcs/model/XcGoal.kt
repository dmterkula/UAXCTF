package com.terkula.uaxctf.statisitcs.model

import com.fasterxml.jackson.annotation.JsonIgnore
import javax.persistence.*

//@Entity
//@Table(name = "xc_season_goals", schema = "uaxc")
//class XcGoal (@Column(name = "runner_id")
//              val runnerId: Int,
//              val time: String,
//              val season: String,
//              val distance: Int){
//
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.AUTO)
//    @JoinColumn
//    val id: Int = 0
//}


@Entity
@Table(name = "xc_season_goals", schema = "uaxc")
class XcGoal (
    @Column(name = "runner_id")
    val runnerId: Int,
    var season: String,
    var type: String,
    var value: String,
    @Column(name = "is_met")
    var isMet: Boolean,
    @Column(name = "track_goal")
    var trackGoal: Boolean,
    var event: String?,
    ) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JoinColumn
    @JsonIgnore
    val id: Int = 0
}