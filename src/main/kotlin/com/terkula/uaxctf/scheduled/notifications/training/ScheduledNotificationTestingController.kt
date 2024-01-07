//package com.terkula.uaxctf.scheduled.notifications.training
//
//import io.swagger.annotations.ApiOperation
//import org.springframework.web.bind.annotation.RequestMapping
//import org.springframework.web.bind.annotation.RequestMethod
//import org.springframework.web.bind.annotation.RestController
//
//@RestController
//class ScheduledNotificationTestingController(val notificationService: ScheduledWeeklyTrainingSummaryNotificationService) {
//
//    @ApiOperation("test sending notifications")
//    @RequestMapping(value = ["testNotifications/"], method = [RequestMethod.GET])
//    fun testNotifications() : String {
//
//        notificationService.sendWeeklyTrainingSummaryNotifications()
//        return "done"
//    }
//
//}