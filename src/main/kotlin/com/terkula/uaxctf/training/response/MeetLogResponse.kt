package com.terkula.uaxctf.training.response

import com.terkula.uaxctf.statisitcs.model.Meet
import com.terkula.uaxctf.training.model.MeetLog

class MeetLogResponse(
        val meetLog: MeetLog?,
        val meet: Meet?
) {
}