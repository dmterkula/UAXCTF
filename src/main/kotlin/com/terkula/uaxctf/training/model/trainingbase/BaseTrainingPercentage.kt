package com.terkula.uaxctf.training.model.trainingbase

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import javax.persistence.*

@Entity
@Table(name = "base_training_percentages", schema = "uaxc")
@JsonInclude(value = JsonInclude.Include.NON_NULL)
class BaseTrainingPercentage (
        @Column(name = "event")
        var event: String,
        @Column(name = "type")
        var type: String,
        @Column(name = "pace_name")
        var paceName: String,
        @Column(name = "percent")
        var percent: Int,
        @Column(name = "season")
        var season: String,
    ) {
        @JsonIgnore
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @JoinColumn
        val id: Int = 0
}