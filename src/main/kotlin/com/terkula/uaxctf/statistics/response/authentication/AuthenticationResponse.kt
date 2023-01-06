package com.terkula.uaxctf.statistics.dto.authentication

import com.fasterxml.jackson.annotation.JsonInclude
import com.terkula.uaxctf.statisitcs.model.AppUser
import com.terkula.uaxctf.statisitcs.model.Runner

@JsonInclude(JsonInclude.Include.NON_NULL)
class AuthenticationResponse(val isAuthenticated: Boolean, val user: AppUser?, val runner: Runner?)

@JsonInclude(JsonInclude.Include.NON_NULL)
class ChangeLoginResponse(val userName: String, val password: String, val success: Boolean)