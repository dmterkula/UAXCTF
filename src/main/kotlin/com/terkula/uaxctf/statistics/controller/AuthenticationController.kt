package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statistics.dto.authentication.AuthenticationResponse
import com.terkula.uaxctf.statistics.service.authentication.AuthenticationService
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@Validated
class AuthenticationController(val authenticationService: AuthenticationService) {

    @ApiOperation("Authenticates a user for the app given a username and password")
    @RequestMapping(value = ["xc/authenticate"], method = [RequestMethod.GET])
    fun getAuthenticate(
            @ApiParam("Username")
            @RequestParam(value = "username", required = true) username: String,

            @ApiParam("Password")
            @RequestParam(value = "password", required = true) password: String,

            ): AuthenticationResponse {

        return authenticationService.authenticate(username, password)
    }

}