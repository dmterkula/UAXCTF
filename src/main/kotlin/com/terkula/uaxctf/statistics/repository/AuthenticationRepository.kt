package com.terkula.uaxctf.statistics.repository

import com.terkula.uaxctf.statisitcs.model.AppUser
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface AuthenticationRepository: CrudRepository<AppUser, Int> {

    fun findByUsernameAndPassword(username: String, password: String): AppUser?

    fun findByUsername(userName: String): AppUser?

}