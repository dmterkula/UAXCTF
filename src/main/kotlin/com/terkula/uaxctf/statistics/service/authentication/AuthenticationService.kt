package com.terkula.uaxctf.statistics.service.authentication

import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.statistics.dto.authentication.AuthenticationResponse
import com.terkula.uaxctf.statistics.repository.AuthenticationRepository
import com.terkula.uaxctf.statistics.repository.RunnerRepository
import org.springframework.stereotype.Service

@Service
class AuthenticationService(
        val authenticationRepository: AuthenticationRepository,
        val runnerRepository: RunnerRepository,
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

}