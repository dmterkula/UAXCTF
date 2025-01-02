package com.terkula.uaxctf.training.model.CrossTraining

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import java.sql.Date
import javax.persistence.*

@Entity
@Table(name = "cross_training", schema = "uaxc")
@JsonInclude(value = JsonInclude.Include.NON_NULL)
class CrossTraining (
        var date: Date,
        var distance: Double?,
        @Column(name = "distance_unit")
        var distanceUnit: String?,
        var duration: String?,
        var icon: String,
        var uuid: String,
        var name: String,
        var description: String?,
        var season: String,
        var team: String,
        @Column(name = "effort_label")
        var effortLabel: String?,
        @Column(name = "cross_training_type")
        var crossTrainingType: String
) {

    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JoinColumn
    val id: Int = 0
}