package com.terkula.uaxctf.statistics.service.authentication

import com.terkula.uaxctf.statisitcs.model.AppUser
import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.statisitcs.model.RunnerAccount
import com.terkula.uaxctf.statistics.dto.authentication.AuthenticationResponse
import com.terkula.uaxctf.statistics.dto.authentication.ChangeLoginResponse
import com.terkula.uaxctf.statistics.repository.AuthenticationRepository
import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.statistics.request.CreateAppUserRequest
import com.terkula.uaxctf.statistics.service.RunnerService
import org.springframework.stereotype.Service

@Service
class AuthenticationService(
        val authenticationRepository: AuthenticationRepository,
        val runnerRepository: RunnerRepository,
        val runnerService: RunnerService
) {

    fun authenticate(username: String, password: String): AuthenticationResponse {

        val user = authenticationRepository.findByUsernameAndPassword(username, password)

        return if (user != null) {
            var runner: Runner? = null
            if (user.role == "runner" && user.runnerId != null) {
                runner = runnerRepository.findById(user.runnerId).orElse(null)
            }
            AuthenticationResponse(true, user, runner)

        } else {
            AuthenticationResponse(false, null, null)
        }

    }

    fun getAllUsernames(): List<String> {
       return authenticationRepository.findAll().map { it.username }
    }

    fun changeLogin(username: String, newUsername: String, password: String): ChangeLoginResponse {

        val user = authenticationRepository.findByUsername(username)

        return if (user != null) {

            user.password = password
            user.username = newUsername
            authenticationRepository.save(user)

            ChangeLoginResponse(newUsername, user.password, true)

        } else {
            ChangeLoginResponse("", "", false)
        }

    }

    fun getAllAppUsers(): List<RunnerAccount> {

        var runners = runnerRepository.findAll().associateBy { it.id }

        return authenticationRepository.findAll()
                .filterNotNull()
                .filter { it.role != "coach" }
                .map {
                    RunnerAccount(runners[it.runnerId]!!, it)
                }
    }

    fun createAppUser(createAppUser: CreateAppUserRequest): AppUser {

        var user = authenticationRepository.findByRunnerId(createAppUser.runnerId)

        if (user == null) {

            val newUser = AppUser(createAppUser.username, createAppUser.password, createAppUser.role, createAppUser.runnerId)

            authenticationRepository.save(newUser)
            return newUser
        } else {
            user.role = createAppUser.role
            user.username = createAppUser.username
            user.password = createAppUser.password
            authenticationRepository.save(user)
            return user
        }

    }

    fun deleteAppUser(deleteAppUser: CreateAppUserRequest): AppUser? {

        var user = authenticationRepository.findByRunnerId(runnerId = deleteAppUser.runnerId)

        if (user != null) {
            authenticationRepository.delete(user)
            return user
        } else {
            return null
        }

    }

    fun getAllRunnersOnRosterWithoutAnAccount(season: String): List<Runner> {

        var runnersOnRoster = runnerService.getRoster(false, season)

        var appUsers = authenticationRepository.findAll()
                .filterNotNull()
                .filter { it.role != "coach" }
                .map {it.runnerId!!}

        return runnersOnRoster.filter {
            !appUsers.contains(it.id)
        }
    }

}