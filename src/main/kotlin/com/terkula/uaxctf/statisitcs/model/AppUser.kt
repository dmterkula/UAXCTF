package com.terkula.uaxctf.statisitcs.model

import javax.persistence.*

@Entity
@Table(name = "app_users", schema = "uaxc")
data class AppUser(
        val username: String,
        val password: String,
        val role: String
        ) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JoinColumn
    val id: Int = 0

}