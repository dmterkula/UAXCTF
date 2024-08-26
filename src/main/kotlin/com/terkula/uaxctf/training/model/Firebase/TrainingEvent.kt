package com.terkula.uaxctf.training.model.Firebase

import com.fasterxml.jackson.annotation.JsonFormat
import java.util.*

class TrainingEvent(
    var id: UUID = UUID.randomUUID(),
    var title: String,
    var date: Date,
    var description: String?,
    var icon: String = "📌",
    var uuid: String,
    var type: Type,
    var distance: Double?,
    var time: String?,
    var minTime: String?,
    var minDistance: Double?,
    var season: String,
    var team: String
)
