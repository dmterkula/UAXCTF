package com.terkula.uaxctf.statistics.exception

import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpServerErrorException

class RunnerNotFoundException(message: String): HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, message) {
}