package com.terkula.uaxctf.statisitcs.model

import com.fasterxml.jackson.annotation.JsonInclude
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "venue_course_profiles", schema = "uaxc")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class VenueCourseProfile(
    @Column(name = "venue_id")
    var venueId: Int,
    var label: String? = null,
    @Column(name = "is_current")
    var isCurrent: Boolean = false,
    @Column(name = "gpx_content")
    var gpxContent: String,
    @Column(name = "distance_meters")
    var distanceMeters: Double? = null,
    @Column(name = "elevation_gain_ft")
    var elevationGainFt: Double? = null,
    @Column(name = "elevation_loss_ft")
    var elevationLossFt: Double? = null,
    @Column(name = "min_elevation_ft")
    var minElevationFt: Double? = null,
    @Column(name = "max_elevation_ft")
    var maxElevationFt: Double? = null,
    @Column(name = "max_grade_pct")
    var maxGradePct: Double? = null,
    @Column(name = "min_grade_pct")
    var minGradePct: Double? = null,
    @Column(name = "course_cost_factor")
    var courseCostFactor: Double? = null,
    @Column(name = "created_at")
    var createdAt: Timestamp? = null
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JoinColumn
    val id: Int = 0
}
