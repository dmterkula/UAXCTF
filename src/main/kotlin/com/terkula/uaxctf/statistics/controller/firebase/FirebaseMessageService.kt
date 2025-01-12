package com.terkula.uaxctf.statistics.controller.firebase

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import com.terkula.uaxctf.statistics.repository.AuthenticationRepository
import org.springframework.stereotype.Service

@Service
class FirebaseMessageService(val authenticationRepository: AuthenticationRepository) {

    companion object {
        // const val DAVIDS_DEVICE_TOKEN = "eEobv0t7OEEClqspB0NbfS:APA91bENOj8NOhhXYnya68A9EJHpH5v6haFHNBVPViZmtUPGVEXo5VWMF0jFwyhXQkRLb1E7St5ZRUYiPj4tzQPcAjcxkHT40U6nHWiIWCWFIciHvJEDVfAJkBB4siZ32-I1pcOfOv4x"
        // const val BENTLEYS_DEVICE_TOKEN = "exTYz1coq0Lspz3shNn1wB:APA91bFPYgv58a2AiNs4WtU14ls4fa1nr1Qi-SW2w8P-wEeiup3tVP30FUhjsJYm5vFQOqUtkW38r2AkRFp7R9K5fz1ifTz0ZFRLt5aw1rVXgw-wv8QtxskvWh_12sgj51LMsWHD24nF"
        const val BENTLEYS_RUNNER_ID = 89
    }

    fun sendMessageToDeviceId(deviceId: String, title: String, message: String, data: Map<String, String>): String {
        val message = Message.builder()
                .setNotification(
                        Notification.builder()
                                .setBody(message)
                                .setTitle(title)
                                .build()
                )
                .setToken(deviceId)
                .putAllData(data)
                .build()

        val response: String = FirebaseMessaging.getInstance().send(message);
        System.out.println("Successfully sent message: " + response);
        return response
    }

    fun sendMessageToAppUser(appUser: Int, title: String, message: String, data: Map<String, String>): String {

       val user = authenticationRepository.findById(appUser)

        if (!user.isPresent) {
            return "user id: " + appUser + " not found"
        }

        if (user.get().deviceId == null) {
            return "app use has no device id"
        }

        val message = Message.builder()
                .setNotification(
                        Notification.builder()
                                .setBody(message)
                                .setTitle(title)
                                .build()
                )
                .setToken(user.get().deviceId)
                .putAllData(data)
                .build()

        val response: String = FirebaseMessaging.getInstance().send(message);
        System.out.println("Successfully sent message: " + response);
        return response
    }

    fun sendMessageToTopic(topic: String, title: String, message: String, data: Map<String, String>): String {

        val message = Message.builder()
                .setNotification(
                        Notification.builder()
                                .setBody(message)
                                .setTitle(title)
                                .build()
                )
                .setTopic(topic)
                .putAllData(data)
                .build()

        val response: String = FirebaseMessaging.getInstance().send(message);
        System.out.println("Successfully sent message: " + response);
        return response
    }
}