package com.terkula.uaxctf.statistics.controller.firebase

import com.terkula.uaxctf.scheduled.notifications.training.ScheduledWeeklyTrainingSummaryNotificationService
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

@RestController
class ScheduledNotificationTestingController(val notificationService: ScheduledWeeklyTrainingSummaryNotificationService) {

    @RequestMapping(value = ["testNotifications/"], method = [RequestMethod.GET])
    fun testNotifications() : String {

        notificationService.sendLongestRunNotification()
        return "done"
    }

}