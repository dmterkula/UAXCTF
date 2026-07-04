package com.terkula.uaxctf.training.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.terkula.uaxctf.statisitcs.model.Venue
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
    var season: String,
    var team: String,
    @Column(name = "use_training_percent")
    var useTrainingPercent: Boolean?,
    @Column(name = "effort_label")
    var effortLabel: String?,
    @Column(name = "venue_id")
    var venueId: Int? = null
) {

    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JoinColumn
    val id: Int = 0

    @Transient
    var venue: Venue? = null
}