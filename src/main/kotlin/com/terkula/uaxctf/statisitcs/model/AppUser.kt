package com.terkula.uaxctf.statisitcs.model

import com.fasterxml.jackson.annotation.JsonIgnore
import javax.persistence.*

@Entity
@Table(name = "app_users", schema = "uaxc")
data class AppUser(
        var username: String,
        var password: String,
        var role: String,
        @Column(name = "runner_id")
        val runnerId: Int?,
        @Column(name = "device_id")
        var deviceId: String?,
        @Column(name = "sub_role")
        var subRole: String?
        ) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JoinColumn
    @JsonIgnore
    val id: Int = 0

}

data class RunnerAccount(var runner: Runner, var appUser: AppUser)