package com.terkula.uaxctf.training.controller

import com.terkula.uaxctf.statistics.controller.firebase.FirebaseMessageService
import com.terkula.uaxctf.statistics.service.RunnerService
import com.terkula.uaxctf.training.request.notifications.SendNotificationRequestToDevice
import com.terkula.uaxctf.training.request.notifications.SendNotificationRequestToRunner
import com.terkula.uaxctf.training.request.notifications.SendNotificationRequestToTopic
import io.swagger.annotations.ApiOperation
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@Validated
class NotificationController(val firebaseMessageService: FirebaseMessageService, val runnerService: RunnerService) {

    @ApiOperation("Sends a notification to a deviceId")
    @RequestMapping(value = ["notifications/send-to-device"], method = [RequestMethod.POST])
    fun sendNotificationToDevice(
            @RequestBody request: SendNotificationRequestToDevice
            ): String {
        return firebaseMessageService.sendMessageToDeviceId(request.deviceId, request.title, request.message, request.data)
    }

    @ApiOperation("Sends notification to runner based on their id")
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

    @ApiOperation("Sends a notification to a topic")
    @RequestMapping(value = ["notifications/send-to-topic"], method = [RequestMethod.POST])
    fun sendNotificationToDevice(
            @RequestBody request: SendNotificationRequestToTopic
    ): String {
        return firebaseMessageService.sendMessageToTopic(request.topic, request.title, request.message, request.data)
    }

}