package com.terkula.uaxctf.statisitcs.model

import java.sql.Date
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "summer_conditioning_config", schema = "uaxc")
class SummerConditioningConfig(

        @Column(name = "year", nullable = false, unique = true, length = 10)
        val year: String,

        @Column(name = "start_date", nullable = false)
        var startDate: Date,

        @Column(name = "end_date", nullable = false)
        var endDate: Date,

        @Column(name = "points_threshold", nullable = false)
        var pointsThreshold: Int = 300,

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
}
