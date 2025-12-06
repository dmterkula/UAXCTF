package com.terkula.uaxctf.statisitcs.model

import com.fasterxml.jackson.annotation.JsonIgnore
import java.sql.Timestamp
import javax.persistence.*

/**
 * SeasonalPointsTracking entity for denormalized tracking of individual runner's
 * seasonal points. This table enables efficient team total calculations without
 * aggregating point_transactions on the fly.
 *
 * Updated whenever a runner earns points with a season/year context.
 */
@Entity
@Table(name = "seasonal_points_tracking", schema = "uaxc")
class SeasonalPointsTracking(
        @Column(name = "runner_id", nullable = false)
        val runnerId: Int,

        @Column(name = "season", nullable = false, length = 20)
        val season: String, // "xc" or "track"

        @Column(name = "year", nullable = false, length = 10)
        val year: String,

        @Column(name = "points_earned", nullable = false)
        var pointsEarned: Int = 0

) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    var id: Long = 0

    @Column(name = "last_updated", nullable = false)
    var lastUpdated: Timestamp = Timestamp(System.currentTimeMillis())

    @PrePersist
    protected fun onCreate() {
        lastUpdated = Timestamp(System.currentTimeMillis())
    }

    @PreUpdate
    protected fun onUpdate() {
        lastUpdated = Timestamp(System.currentTimeMillis())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SeasonalPointsTracking) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "SeasonalPointsTracking(id=$id, runnerId=$runnerId, season='$season', " +
                "year=$year, pointsEarned=$pointsEarned)"
    }
}
