package com.terkula.uaxctf.statistics.response

import com.terkula.uaxctf.statistics.dto.AggregatePRStatsDTO

class AggregateStatsResponse(var aggregatePRStats: AggregatePRStatsDTO, var totalSplits: Int, var total5Ks: Int, var totalRunners: Int)