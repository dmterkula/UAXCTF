package com.terkula.uaxctf.statistics.dto.leaderboard

import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.statistics.dto.TimeTrialDTO

class RankedTryoutDTO(val runner: Runner, val rank: Int, val tryout: TimeTrialDTO)