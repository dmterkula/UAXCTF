package com.terkula.uaxctf.training.request.notifications

class SendNotificationRequestToDevice(val deviceId: String, val title: String, val message: String, val data: Map<String, String>) {
}

class SendNotificationRequestToAppUser(val userId: Int, val title: String, val message: String, val data: Map<String, String>) {
}

class SendNotificationRequestToTopic(val topic: String, val title: String, val message: String, val data: Map<String, String>) {
}

class SendNotificationRequestToRunner(val runnerId: Int, val title: String, val message: String, val data: Map<String, String>) {
}