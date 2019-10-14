package com.terkula.uaxctf.training.model

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "raw_workout_split", schema = "uaxc")

class RawWorkout (
        @Id
        @Column(name = "name")
        val name: String,
        @Column(name = "time")
        val time: String,
        @Column(name = "split_number")
        val splitNumber: Int)