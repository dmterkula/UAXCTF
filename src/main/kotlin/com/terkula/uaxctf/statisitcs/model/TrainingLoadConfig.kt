package com.terkula.uaxctf.statisitcs.model

import javax.persistence.*
import java.sql.Timestamp

@Entity
@Table(name = "training_load_config", schema = "uaxc")
class TrainingLoadConfig(
    @Column(name = "config_key", nullable = false, length = 100)
    var configKey: String,

    @Column(name = "team", length = 10)
    var team: String? = null,

    @Column(name = "value", nullable = false)
    var value: Double,

    @Column(name = "description", length = 500)
    var description: String? = null
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Timestamp = Timestamp(System.currentTimeMillis())

    @PreUpdate
    protected fun onUpdate() {
        updatedAt = Timestamp(System.currentTimeMillis())
    }
}
