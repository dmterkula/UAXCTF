package com.terkula.uaxctf.training.request.crosstraining

import com.fasterxml.jackson.annotation.JsonFormat
import java.sql.Timestamp

class CreateCommentRequest(
        var uuid: String,
        var referenceUuid: String,
        var madeBy: String,
        var message: String,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
        var timestamp: Timestamp
) {
}