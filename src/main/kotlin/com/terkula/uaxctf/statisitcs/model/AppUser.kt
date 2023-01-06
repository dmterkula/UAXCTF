package com.terkula.uaxctf.statisitcs.model

import com.fasterxml.jackson.annotation.JsonIgnore
import javax.persistence.*

@Entity
@Table(name = "app_users", schema = "uaxc")
data class AppUser(
        var username: String,
        var password: String,
        val role: String,
        @Column(name = "runner_id")
        @JsonIgnore
        val runnerId: Int?
        ) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JoinColumn
    @JsonIgnore
    val id: Int = 0

}