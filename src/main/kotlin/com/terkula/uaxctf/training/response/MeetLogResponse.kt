package com.terkula.uaxctf.training.response

import com.terkula.uaxctf.statisitcs.model.Meet
import com.terkula.uaxctf.training.model.MeetLog
import com.terkula.uaxctf.training.model.PreMeetLog
import com.terkula.uaxctf.training.model.TrainingComment

class MeetLogResponse(
        val meetLog: MeetLog?,
        val meet: Meet?,
        val preMeetLog: PreMeetLog?,
        val preMeetLogComments: List<TrainingComment>,
        val postMeetLogComments: List<TrainingComment>
) {
}

class PreMeetLogResponse(
        val xcPreMeetLog: PreMeetLog?,
        val meet: Meet?
) {
}