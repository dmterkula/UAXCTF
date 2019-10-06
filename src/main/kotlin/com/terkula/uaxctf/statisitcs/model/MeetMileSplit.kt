package com.terkula.uaxctf.statisitcs.model

import com.fasterxml.jackson.annotation.JsonIgnore
import javax.persistence.*

@Entity
@Table(name = "xc_meet_splits", schema = "uaxc")
class MeetMileSplit (
                      @Column(name = "meet_id")
                      val meetId: Int,
                      @Column(name = "runner_id")
                      val runnerId: Int,
                      @Column(name = "first_mile")
                      val mileOne: String,
                      @Column(name = "second_mile")
                      val mileTwo: String,
                      @Column(name = "third_mile")
                      val mileThree: String) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    val id: Int = 0
}