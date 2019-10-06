package com.terkula.uaxctf.statisitcs.model

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "raw_meet_splits", schema = "uaxc")
data class RawMileSplit(
        @Id
        val name: String,
        @Column(name = "mile_one")
        val mileOne: String,
        @Column(name = "mile_two")
        val mileTwo: String,
        @Column (name = "mile_two_total")
        val mileTwoTotal: String,
        @Column(name = "mile_three")
        val mileThree: String,
        @Column(name = "finish")
        val finishTime: String)