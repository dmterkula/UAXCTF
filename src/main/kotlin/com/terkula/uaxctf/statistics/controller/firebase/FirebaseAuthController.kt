package com.terkula.uaxctf.statistics.controller.firebase

import com.google.auth.oauth2.GoogleCredentials
import com.terkula.uaxctf.training.response.firebaseauth.FirebaseAuthTokenResponse
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.io.ByteArrayInputStream
import java.sql.Date

@RestController
class FirebaseAuthController(val firebaseMessageService: FirebaseMessageService) {

    @RequestMapping(value = ["firebase/generateAuthToken/"], method = [RequestMethod.GET])
    fun getAuthToken() : FirebaseAuthTokenResponse {

        var inputStream = ByteArrayInputStream(FirebaseAuthService.FIREBASE_KEY.toByteArray())

        val googleCredentials: GoogleCredentials = GoogleCredentials
                .fromStream(inputStream)
                .createScoped(FirebaseAuthService.FIREBASE_MESSAGING_SCOPES)

        googleCredentials.refresh()
        return FirebaseAuthTokenResponse(googleCredentials.getAccessToken().getTokenValue())

    }

//    @RequestMapping(value = ["firebase/sendMessageToDevice/"], method = [RequestMethod.GET])
//    fun sendMessageToDevice(
//            @RequestParam(value = "deviceId", required = true) deviceId: String,
//            @RequestParam(value = "title", required = true) title: String,
//            @RequestParam(value = "message", required = true) message: String,
//    ) : String {
//
//        return firebaseMessageService.sendMessageToDeviceId(deviceId, title, message, emptyMap())
//    }

}