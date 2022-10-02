package com.terkula.uaxctf.statistics.service.authentication

import com.terkula.uaxctf.statistics.dto.authentication.AuthenticationResponse
import com.terkula.uaxctf.statistics.repository.AuthenticationRepository
import org.springframework.stereotype.Service

@Service
class AuthenticationService(val authenticationRepository: AuthenticationRepository) {

    fun authenticate(username: String, password: String): AuthenticationResponse {

        val user = authenticationRepository.findByUsernameAndPassword(username, password)

        return if (user != null) {
            AuthenticationResponse(true, user)
        } else {
            AuthenticationResponse(false, null)
        }

    }

}