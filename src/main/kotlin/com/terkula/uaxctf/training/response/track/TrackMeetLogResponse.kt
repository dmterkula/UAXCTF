package com.terkula.uaxctf.training.response.track

import com.terkula.uaxctf.statisitcs.model.track.TrackMeet
import com.terkula.uaxctf.training.model.TrackMeetLog

class TrackMeetLogResponse(val trackMeetLogs: List<TrackMeetLog>,
                           val trackMeet: TrackMeet?) {

}