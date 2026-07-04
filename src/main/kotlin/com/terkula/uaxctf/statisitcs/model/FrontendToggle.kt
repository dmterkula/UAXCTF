package com.terkula.uaxctf.statisitcs.model

import javax.persistence.*
import java.sql.Timestamp

@Entity
@Table(name = "frontend_toggles", schema = "uaxc")
class FrontendToggle(
    @Column(name = "toggle_name", nullable = false, length = 100)
    var toggleName: String,

    @Column(name = "team", length = 10)
    var team: String? = null,

    @Column(name = "enabled", nullable = false)
    var enabled: Boolean = false,

    @Column(name = "description", length = 500)
    var description: String? = null
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0

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
