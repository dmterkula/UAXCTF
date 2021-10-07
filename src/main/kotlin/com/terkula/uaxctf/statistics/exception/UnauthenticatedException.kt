package com.terkula.uaxctf.statistics.exception

import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException

class UnauthenticatedException(val description: String): HttpClientErrorException(HttpStatus.UNAUTHORIZED, description) {
}