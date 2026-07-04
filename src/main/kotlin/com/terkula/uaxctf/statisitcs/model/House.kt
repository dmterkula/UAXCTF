package com.terkula.uaxctf.statisitcs.model

import javax.persistence.*
import java.sql.Timestamp

@Entity
@Table(name = "houses", schema = "uaxc")
class House(
    @Column(name = "name", nullable = false, length = 100)
    var name: String,

    @Column(name = "season", nullable = false, length = 20)
    val season: String,

    @Column(name = "year", nullable = false, length = 10)
    val year: String,

    @Column(name = "created_by", length = 100)
    val createdBy: String? = null
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Timestamp = Timestamp(System.currentTimeMillis())

    @PrePersist
    protected fun onCreate() {
        createdAt = Timestamp(System.currentTimeMillis())
    }
}
