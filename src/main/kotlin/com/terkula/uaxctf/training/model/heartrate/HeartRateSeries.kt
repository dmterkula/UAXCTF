package com.terkula.uaxctf.training.model.heartrate

import com.fasterxml.jackson.annotation.JsonIgnore
import javax.persistence.*

@Entity
@Table(name = "heart_rate_series", schema = "uaxc")
class HeartRateSeries(
        @Column(name = "activity_type")
        var activityType: String,
        @Column(name = "activity_uuid")
        var activityUuid: String,
        @Column(name = "runner_id")
        var runnerId: Int,
        @Column(name = "sample_count")
        var sampleCount: Int,
        @Column(name = "duration_seconds")
        var durationSeconds: Int,
        @Column(name = "avg_hr")
        var avgHr: Int?,
        @Column(name = "max_hr")
        var maxHr: Int?,
        @Column(name = "min_hr")
        var minHr: Int?,
        @Column(name = "samples", columnDefinition = "LONGTEXT")
        var samples: String,
        @Column(name = "source")
        var source: String?,
        @Column(name = "pace_samples", columnDefinition = "LONGTEXT")
        var paceSamples: String? = null,
) {

    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JoinColumn
    val id: Int = 0
}
