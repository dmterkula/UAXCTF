package com.terkula.uaxctf.statistics.response

import com.terkula.uaxctf.statisitcs.model.MeetMileSplit

class MileSplitDataLoadResponse (val successes: List<MeetMileSplit>, val errors: List<List<Any>>)