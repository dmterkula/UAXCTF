package com.terkula.uaxctf.statisitcs.model

import com.fasterxml.jackson.annotation.JsonIgnore
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "points_configuration", schema = "uaxc")
class PointsConfiguration(
        @Column(name = "config_key", unique = true, nullable = false, length = 100)
        var configKey: String,

        @Column(name = "config_value", nullable = false)
        var configValue: Int,

        @Column(name = "description", columnDefinition = "TEXT")
        var description: String? = null,

        @Column(name = "updated_by", length = 100)
        var updatedBy: String? = null

) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    val id: Int = 0

    @Column(name = "last_updated")
    var lastUpdated: Timestamp = Timestamp(System.currentTimeMillis())

    @PrePersist
    protected fun onCreate() {
        lastUpdated = Timestamp(System.currentTimeMillis())
    }

    @PreUpdate
    protected fun onUpdate() {
        lastUpdated = Timestamp(System.currentTimeMillis())
    }
}