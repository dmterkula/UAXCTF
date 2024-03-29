package com.terkula.uaxctf.training.model

import com.fasterxml.jackson.annotation.JsonIgnore
import java.sql.Date
import javax.persistence.*

@Entity
@Table(name = "xc_training_runs", schema = "uaxc")
class TrainingRun (
    var date: Date,
    var distance: Double?,
    var time: String?,
    var icon: String,
    var uuid: String,
    var name: String,
    @Column(name = "min_time")
    var minTime: String?,
    @Column(name = "min_distance")
    var minDistance: Double?,
    var season: String
) {

    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JoinColumn
    val id: Int = 0
}