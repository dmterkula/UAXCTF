package com.terkula.uaxctf.statisitcs.model

import java.sql.Timestamp
import javax.persistence.*

/**
 * SeasonalReward entity representing team-based seasonal reward configurations.
 * Coaches create reward tiers with point thresholds that the entire active roster
 * works toward collectively. When the team reaches a threshold, the reward is
 * automatically marked as achieved.
 */
@Entity
@Table(name = "seasonal_rewards", schema = "uaxc")
class SeasonalReward(
        @Column(name = "season", nullable = false, length = 20)
        val season: String, // "xc" or "track"

        @Column(name = "year", nullable = false, length = 10)
        val year: String,

        @Column(name = "reward_name", nullable = false, length = 255)
        var rewardName: String,

        @Column(name = "description", nullable = false, columnDefinition = "TEXT")
        var description: String,

        @Column(name = "point_threshold", nullable = false)
        var pointThreshold: Int,

        @Column(name = "display_order", nullable = false)
        var displayOrder: Int = 0,

        @Column(name = "is_achieved")
        var isAchieved: Boolean = false,

        @Column(name = "achieved_date")
        var achievedDate: Timestamp? = null,

        @Column(name = "created_by", length = 100)
        val createdBy: String? = null

) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Timestamp = Timestamp(System.currentTimeMillis())

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Timestamp = Timestamp(System.currentTimeMillis())

    @PrePersist
    protected fun onCreate() {
        val now = Timestamp(System.currentTimeMillis())
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    protected fun onUpdate() {
        updatedAt = Timestamp(System.currentTimeMillis())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SeasonalReward) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "SeasonalReward(id=$id, season='$season', year=$year, rewardName='$rewardName', description='$description', " +
                "pointThreshold=$pointThreshold, displayOrder=$displayOrder, isAchieved=$isAchieved)"
    }
}
