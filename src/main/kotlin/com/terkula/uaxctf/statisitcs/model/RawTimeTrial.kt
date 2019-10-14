package com.terkula.uaxctf.statisitcs.model

import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "time_trial_raw", schema = "uaxc")
class RawTimeTrial (
        @Id
        val name: String,
        val time: String,
        val place: Int) {

}