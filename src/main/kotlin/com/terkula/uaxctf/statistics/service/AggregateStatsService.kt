package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statistics.repository.MeetMileSplitRepository
import com.terkula.uaxctf.statistics.repository.MeetPerformanceRepository
import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.statistics.response.AggregateStatsResponse
import org.springframework.stereotype.Service

@Service
class AggregateStatsService(
        var runnerRepository: RunnerRepository,
        var meetPerformanceRepository: MeetPerformanceRepository,
        var splitRepository: MeetMileSplitRepository,
        var personalRecordService: PersonalRecordService
) {


    fun getAggregateStats(): AggregateStatsResponse {

        val totalRunnerCount = runnerRepository.count()
        val totalMeetPerformances = meetPerformanceRepository.count()
        val totalSplitCount = splitRepository.count()
        val prAggregatePRStatsDTO = personalRecordService.getAggregatePRStats()

        return AggregateStatsResponse(prAggregatePRStatsDTO, totalSplitCount.toInt(), totalMeetPerformances.toInt(), totalRunnerCount.toInt())

    }

}