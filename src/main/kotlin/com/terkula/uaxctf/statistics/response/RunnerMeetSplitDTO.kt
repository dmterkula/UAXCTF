package com.terkula.uaxctf.statistics.response

import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.statistics.dto.RunnerMeetSplitDTO

class RunnerMeetSplitResponse (val runner: Runner, val mileSplits: List<RunnerMeetSplitDTO>)