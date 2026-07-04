package com.terkula.uaxctf.statisitcs.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import java.sql.Date
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(
    name = "why_i_can_activity_links",
    schema = "uaxc",
    uniqueConstraints = [UniqueConstraint(columnNames = ["runner_id", "activity_type", "activity_uuid"])]
)
@JsonInclude(value = JsonInclude.Include.NON_NULL)
class WhyICanActivityLink(
    @Column(name = "uuid", unique = true, nullable = false)
    var uuid: String,

    @Column(name = "runner_id", nullable = false)
    var runnerId: Int,

    @Column(name = "activity_type", nullable = false)
    var activityType: String,

    @Column(name = "activity_uuid", nullable = false)
    var activityUuid: String,

    @Column(name = "display_title", nullable = false, length = 500)
    var displayTitle: String,

    @Column(name = "activity_date", nullable = false)
    var activityDate: Date,

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Timestamp = Timestamp(System.currentTimeMillis()),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Timestamp = Timestamp(System.currentTimeMillis())
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    val id: Int = 0

    @PrePersist
    protected fun onCreate() {
        createdAt = Timestamp(System.currentTimeMillis())
        updatedAt = Timestamp(System.currentTimeMillis())
    }

    @PreUpdate
    protected fun onUpdate() {
        updatedAt = Timestamp(System.currentTimeMillis())
    }
}
