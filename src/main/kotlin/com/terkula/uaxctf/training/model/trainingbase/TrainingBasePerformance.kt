package com.terkula.uaxctf.training.model.trainingbase

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import javax.persistence.*

@Entity
@Table(name = "runners_training_base_performance", schema = "uaxc")
@JsonInclude(value = JsonInclude.Include.NON_NULL)
class TrainingBasePerformance (
        @Column(name = "runner_id")
        var runnerId: Int,
        @Column(name = "fraction_of_miles")
        var fractionOfMiles: Double,
        @Column(name = "seconds")
        var seconds: Int,
        @Column(name = "season")
        var season: String,
        @Column(name = "event")
        var event: String,
        @Column(name = "year")
        var year: String,
        @Column(name = "uuid")
        var uuid: String
) {

    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JoinColumn
    val id: Int = 0

}