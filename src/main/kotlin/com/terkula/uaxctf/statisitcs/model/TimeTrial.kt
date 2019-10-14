package com.terkula.uaxctf.statisitcs.model

import com.fasterxml.jackson.annotation.JsonIgnore
import javax.persistence.*

@Entity
@Table(name = "time_trial_result", schema = "uaxc")
class TimeTrial(
        @Column(name = "runner_id")
        val runnerId: Int,
        val time: String,
        val place: Int,
        val season: String) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    val id: Int = 0

}

