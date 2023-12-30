package com.terkula.uaxctf.statistics.controller.firebase

import com.google.auth.oauth2.GoogleCredentials
import com.terkula.uaxctf.training.response.firebaseauth.FirebaseAuthTokenResponse
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.io.ByteArrayInputStream
import java.sql.Date

@RestController
class FirebaseAuthController(val firebaseMessageService: FirebaseMessageService) {

    @ApiOperation("get firebase auth token")
    @RequestMapping(value = ["firebase/generateAuthToken/"], method = [RequestMethod.GET])
    fun getAuthToken() : FirebaseAuthTokenResponse {

        var inputStream = ByteArrayInputStream(FirebaseAuthService.FIREBASE_KEY.toByteArray())

        val googleCredentials: GoogleCredentials = GoogleCredentials
                .fromStream(inputStream)
                .createScoped(FirebaseAuthService.FIREBASE_MESSAGING_SCOPES)

        googleCredentials.refresh()
        return FirebaseAuthTokenResponse(googleCredentials.getAccessToken().getTokenValue())

    }

//    @ApiOperation("send message to device")
//    @RequestMapping(value = ["firebase/sendMessageToDevice/"], method = [RequestMethod.GET])
//    fun sendMessageToDevice(
//            @ApiParam("deviceId")
//            @RequestParam(value = "deviceId", required = true) deviceId: String,
//            @ApiParam("title")
//            @RequestParam(value = "title", required = true) title: String,
//            @ApiParam("message")
//            @RequestParam(value = "message", required = true) message: String,
//    ) : String {
//
//        return firebaseMessageService.sendMessageToDeviceId(deviceId, title, message, emptyMap())
//    }

}