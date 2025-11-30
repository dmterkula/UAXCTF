package com.terkula.uaxctf.statistics.repository

import com.terkula.uaxctf.statisitcs.model.AppUser
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface AuthenticationRepository: CrudRepository<AppUser, Int> {

    fun findByUsernameAndPassword(username: String, password: String): AppUser?

    fun findByDeviceId(deviceId: String): AppUser?

    fun findByUsername(userName: String): AppUser?

    fun deleteByUsernameAndPassword(username: String, password: String): AppUser?

    fun findByRunnerId(runnerId: Int): AppUser?

    fun deleteByRunnerId(runnerId: Int): AppUser?

    fun findByRole(role: String): List<AppUser>

}