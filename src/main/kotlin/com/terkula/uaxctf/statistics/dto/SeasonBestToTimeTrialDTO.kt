package com.terkula.uaxctf.statistics.dto

import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.statistics.dto.leaderboard.SeasonBestDTO
import com.terkula.uaxctf.util.calculateSecondsFrom

class SeasonBestToTimeTrialDTO(val previousYearSeasonBest: SeasonBestDTO, val timeTrial: String) {

    fun getDifference(): Double {
        return timeTrial.calculateSecondsFrom() - previousYearSeasonBest.seasonBest.first().time.calculateSecondsFrom()
    }

    fun getRunner(): Runner {
        return this.previousYearSeasonBest.runner
    }

}