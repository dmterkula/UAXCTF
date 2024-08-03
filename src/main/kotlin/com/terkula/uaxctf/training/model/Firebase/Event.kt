package com.terkula.uaxctf.training.model.Firebase

import java.util.*

open class Event (
    var id: UUID = UUID.randomUUID(),
    var title: String,
    var date: Date,
    var description: String?,
    var icon: String = "📌",
    var uuid: String,
    var type: String) {
}