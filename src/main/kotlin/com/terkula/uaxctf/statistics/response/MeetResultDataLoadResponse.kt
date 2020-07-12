package com.terkula.uaxctf.statistics.response

import com.terkula.uaxctf.statisitcs.model.RaceResult

class MeetResultDataLoadResponse (val successes: List<RaceResult>, val errors: List<RaceResult>) {
}