package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statisitcs.model.AppUser
import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.statisitcs.model.RunnerAccount
import com.terkula.uaxctf.statistics.dto.authentication.AuthenticationResponse
import com.terkula.uaxctf.statistics.dto.authentication.ChangeLoginResponse
import com.terkula.uaxctf.statistics.request.CreateAppUserRequest
import com.terkula.uaxctf.statistics.service.authentication.AuthenticationService
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@Validated
class AuthenticationController(val authenticationService: AuthenticationService) {

    @RequestMapping(value = ["xc/authenticate"], method = [RequestMethod.GET])
    fun getAuthenticate(
            @RequestParam(value = "username", required = true) username: String,

            @RequestParam(value = "password", required = true) password: String,

            @RequestParam(value = "deviceId", required = false) deviceId: String?

            ): AuthenticationResponse {

        return authenticationService.authenticate(username, password, deviceId)
    }

    @RequestMapping(value = ["xc/authenticate/device"], method = [RequestMethod.GET])
    fun getAuthenticate(

        @RequestParam(value = "deviceId", required = false) deviceId: String

    ): AuthenticationResponse {

        return authenticationService.authenticateDevice(deviceId)
    }

    @RequestMapping(value = ["xc/authenticate/change-credentials"], method = [RequestMethod.POST])
    fun changeLogin(
            @RequestParam(value = "username", required = true) username: String,

            @RequestParam(value = "newUsername", required = true) newUsername: String,

            @RequestParam(value = "password", required = true) password: String,

            ): ChangeLoginResponse {

        return authenticationService.changeLogin(username, newUsername, password)
    }

    @RequestMapping(value = ["xc/authenticate/get-all-usernames"], method = [RequestMethod.GET])
    fun changeLogin(

            ): List<String> {
        return authenticationService.getAllUsernames()
    }

    @RequestMapping(value = ["xc/appUsers/runners"], method = [RequestMethod.GET])
    fun getAllAppUsers(

    ): List<RunnerAccount> {
        return authenticationService.getAllAppUsers()
    }

    @RequestMapping(value = ["xc/appUsers/create"], method = [RequestMethod.PUT])
    fun createUser(
            @RequestBody createAppUserRequest: CreateAppUserRequest
    ): AppUser {

        return authenticationService.createAppUser(createAppUserRequest)
    }

    @RequestMapping(value = ["xc/appUsers/delete"], method = [RequestMethod.DELETE])
    fun deleteUser(
            @RequestBody deleteAppUserRequest: CreateAppUserRequest
    ): AppUser? {

        return authenticationService.deleteAppUser(deleteAppUserRequest)
    }


    @RequestMapping(value = ["xc/appUsers/runnersWithoutAccount"], method = [RequestMethod.GET])
    fun getRunnersOnRosterWithoutAccount(
            @RequestParam(value = "season", required = true) season: String,
    ): List<Runner> {
        return authenticationService.getAllRunnersOnRosterWithoutAnAccount(season)
    }

    @RequestMapping(value = ["xc/appUsers/AllForSeason"], method = [RequestMethod.GET])
    fun getRunnersOnRosterWithoutAccount(
            @RequestParam(value = "season", required = true) season: String,
            @RequestParam(value = "year", required = true) year: String,
    ): List<AppUser> {
        return authenticationService.getAllAppUsersForSeason(year, season)
    }
}