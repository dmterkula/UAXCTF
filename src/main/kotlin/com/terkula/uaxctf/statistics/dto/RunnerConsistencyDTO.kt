package com.terkula.uaxctf.statistics.dto

import com.terkula.uaxctf.statisitcs.model.ConsistencyRank
import com.terkula.uaxctf.statisitcs.model.Runner

class RunnerConsistencyDTO (val runner: Runner, val consistencyRank: ConsistencyRank) {

}

class RankedRunnerConsistencyDTO(val runner: Runner, val consistencyValue: Double, val rank: Int) {

}
