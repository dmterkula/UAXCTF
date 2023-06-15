package com.terkula.uaxctf.statistics.response

import com.terkula.uaxctf.statistics.dto.leaderboard.SeasonBestDTO

class SeasonBestResponse(val count: Int, val seasonBests: List<SeasonBestDTO>)