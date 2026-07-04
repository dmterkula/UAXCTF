package com.terkula.uaxctf.statisitcs.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "why_i_can", schema = "uaxc")
@JsonInclude(value = JsonInclude.Include.NON_NULL)
class WhyICan(
    @Column(name = "uuid", unique = true, nullable = false)
    var uuid: String,

    @Column(name = "runner_id", nullable = false)
    var runnerId: Int,

    @Column(name = "text", columnDefinition = "TEXT", nullable = false)
    var text: String,

    @Column(name = "size", nullable = false)
    var size: Int = 3,

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
