package com.terkula.uaxctf.training.controller

import com.terkula.uaxctf.statistics.controller.firebase.FirebaseMessageService
import com.terkula.uaxctf.statistics.repository.AuthenticationRepository
import com.terkula.uaxctf.statistics.service.RunnerService
import com.terkula.uaxctf.training.request.notifications.SendNotificationRequestToAppUser
import com.terkula.uaxctf.training.request.notifications.SendNotificationRequestToDevice
import com.terkula.uaxctf.training.request.notifications.SendNotificationRequestToRunner
import com.terkula.uaxctf.training.request.notifications.SendNotificationRequestToTopic
import com.terkula.uaxctf.training.request.notifications.SendNotificationRequestToUsername
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@Validated
class NotificationController(val firebaseMessageService: FirebaseMessageService, val runnerService: RunnerService, val authenticationRepository: AuthenticationRepository) {

    @RequestMapping(value = ["notifications/send-to-device"], method = [RequestMethod.POST])
    fun sendNotificationToDevice(
            @RequestBody request: SendNotificationRequestToDevice
            ): String {
        return firebaseMessageService.sendMessageToDeviceId(request.deviceId, request.title, request.message, request.data)
    }

    @RequestMapping(value = ["notifications/send-to-user"], method = [RequestMethod.POST])
    fun sendNotificationToAppUser(
            @RequestBody request: SendNotificationRequestToAppUser
    ): String {
        return firebaseMessageService.sendMessageToAppUser(request.userId, request.title, request.message, request.data)
    }

    @RequestMapping(value = ["notifications/send-to-runner"], method = [RequestMethod.POST])
    fun sendNotificationToDevice(
            @RequestBody request: SendNotificationRequestToRunner
    ): String {

        val runner = runnerService.runnerRepository.findById(request.runnerId).get()

        return if (runner.deviceId != null) {
            firebaseMessageService.sendMessageToDeviceId(runner.deviceId!!, request.title, request.message, request.data)
        } else {
            "No device to send message to"
        }
    }

    @RequestMapping(value = ["notifications/send-to-username"], method = [RequestMethod.POST])
    fun sendNotificationToUsername(
            @RequestBody request: SendNotificationRequestToUsername
    ): String {
        val user = authenticationRepository.findByUsername(request.username)
            ?: return "no user with username: ${request.username}"
        val deviceId = user.deviceId
        if (deviceId.isNullOrEmpty()) {
            return "user ${request.username} has no device id"
        }
        return firebaseMessageService.sendMessageToDeviceId(deviceId, request.title, request.message, request.data)
    }

    @RequestMapping(value = ["notifications/send-to-topic"], method = [RequestMethod.POST])
    fun sendNotificationToDevice(
            @RequestBody request: SendNotificationRequestToTopic
    ): String {
        return firebaseMessageService.sendMessageToTopic(request.topic, request.title, request.message, request.data)
    }

}