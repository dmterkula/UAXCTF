package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statisitcs.model.AppUser
import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.statisitcs.model.RunnerAccount
import com.terkula.uaxctf.statistics.dto.authentication.AuthenticationResponse
import com.terkula.uaxctf.statistics.dto.authentication.ChangeLoginResponse
import com.terkula.uaxctf.statistics.request.CreateAppUserRequest
import com.terkula.uaxctf.statistics.service.authentication.AuthenticationService
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

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

    @ApiOperation("Changes password for user")
    @RequestMapping(value = ["xc/authenticate/change-credentials"], method = [RequestMethod.POST])
    fun changeLogin(
            @ApiParam("Username")
            @RequestParam(value = "username", required = true) username: String,

            @ApiParam("new username")
            @RequestParam(value = "newUsername", required = true) newUsername: String,

            @ApiParam("Password")
            @RequestParam(value = "password", required = true) password: String,

            ): ChangeLoginResponse {

        return authenticationService.changeLogin(username, newUsername, password)
    }

    @ApiOperation("Get all usernames")
    @RequestMapping(value = ["xc/authenticate/get-all-usernames"], method = [RequestMethod.GET])
    fun changeLogin(

            ): List<String> {
        return authenticationService.getAllUsernames()
    }

    @ApiOperation("Get all runner accounts")
    @RequestMapping(value = ["xc/appUsers/runners"], method = [RequestMethod.GET])
    fun getAllAppUsers(

    ): List<RunnerAccount> {
        return authenticationService.getAllAppUsers()
    }

    @ApiOperation("Create App User")
    @RequestMapping(value = ["xc/appUsers/create"], method = [RequestMethod.PUT])
    fun createUser(
            @RequestBody createAppUserRequest: CreateAppUserRequest
    ): AppUser {

        return authenticationService.createAppUser(createAppUserRequest)
    }

    @ApiOperation("Create App User")
    @RequestMapping(value = ["xc/appUsers/delete"], method = [RequestMethod.DELETE])
    fun deleteUser(
            @RequestBody deleteAppUserRequest: CreateAppUserRequest
    ): AppUser? {

        return authenticationService.deleteAppUser(deleteAppUserRequest)
    }


    @ApiOperation("Create App User")
    @RequestMapping(value = ["xc/appUsers/runnersWithoutAccount"], method = [RequestMethod.GET])
    fun getRunnersOnRosterWithoutAccount(
            @RequestParam(value = "season", required = true) season: String,
    ): List<Runner> {
        return authenticationService.getAllRunnersOnRosterWithoutAnAccount(season)
    }
}