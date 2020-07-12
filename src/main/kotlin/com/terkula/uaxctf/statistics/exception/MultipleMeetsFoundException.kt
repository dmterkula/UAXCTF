package com.terkula.uaxctf.statistics.exception

import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException

class MultipleMeetsFoundException(message: String): HttpClientErrorException(HttpStatus.BAD_REQUEST, message) {
}