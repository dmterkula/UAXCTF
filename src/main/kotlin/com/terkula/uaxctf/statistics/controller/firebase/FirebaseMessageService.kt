package com.terkula.uaxctf.statistics.controller.firebase

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import org.springframework.stereotype.Service

@Service
class FirebaseMessageService {

    companion object {
        const val DAVIDS_DEVICE_TOKEN = "eHgXoMfVw05sta4UZwt76o:APA91bEsINy_vYQ31PlvpAjiAKXyYgTFABt-rPCp84rdzks4Ov-Kz0EDhMVQM_Ir7hWsN_HXbu2btbbyMEXfcHqY4aBk_pzg-q9qyxsaXAbxsGesPh-jpfxbfLUNxKGOy2XKKBTiLrc0"
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