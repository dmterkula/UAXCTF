package com.terkula.uaxctf.training.request.journal

import com.fasterxml.jackson.annotation.JsonFormat
import java.sql.Timestamp
import javax.persistence.Column

class CreateJournalCommentRequest(
        var uuid: String,
        var journalUuid: String,
        var madeBy: String,
        var message: String,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
        var timestamp: Timestamp
) {
}