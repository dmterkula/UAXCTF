package com.terkula.uaxctf.training.model

import com.fasterxml.jackson.annotation.JsonIgnore
import javax.persistence.*

@Entity
@Table(name = "xc_training_run_records", schema = "uaxc")
class RunnersTrainingRun(
    @Column(name = "training_run_uuid")
    val trainingRunUuid: String,
    @Column(name = "runner_id")
    val runnerId: Int,
    var time: String,
    var distance: Double,
    @Column(name = "avg_pace")
    var avgPace: String,
    val uuid: String,
    var notes: String?
) {

    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JoinColumn
    val id: Int = 0
}