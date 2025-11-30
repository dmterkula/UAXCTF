package com.terkula.uaxctf.training.model.liveworkout

import com.fasterxml.jackson.annotation.JsonInclude
import java.sql.Timestamp
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "live_workouts", schema = "uaxc")
@JsonInclude(value = JsonInclude.Include.NON_NULL)
class LiveWorkout (
    @Id
    var uuid: String,
    @Column(name = "workout_uuid")
    var workoutUuid: String,
    var timestamp: Timestamp,
    var username: String,
    @Column(name = "heart_rate")
    var heartRate: Double,
    @Column(name = "avg_heart_rate")
    var avgHeartRate: Double,
    @Column(name = "active_energy")
    var activeEnergy: Double,
    @Column(name = "vo2_max")
    var vo2Max: Double,
    var distance: Double,
    @Column(name = "elapsed_time")
    var elapsedTime: Double,
    @Column(name = "avg_pace")
    var avgPace: Double,
    @Column(name = "current_pace")
    var currentPace: Double,
    @Column(name = "avg_power")
    var avgPower: Double,
    @Column(name = "current_power")
    var currentPower: Double,
    @Column(name = "predicted_marathon")
    var predictedMarathon: String,
    @Column(name = "rolling_pace")
    var rollingPace: String,
    @Column(name = "song_title")
    var songTitle: String,
    @Column(name = "song_artist")
    var songArtist: String,
    @Column(name = "mile_splits")
    var mileSplits: String

)