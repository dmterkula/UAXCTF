package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statistics.dto.MeetSplitsDTO
import com.terkula.uaxctf.statistics.dto.RunnerMeetSplitDTO
import com.terkula.uaxctf.statistics.exception.RunnerNotFoundByPartialNameException
import com.terkula.uaxctf.statistics.repository.MeetMileSplitRepository
import com.terkula.uaxctf.statistics.repository.MeetRepository
import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.statistics.request.SortingMethodContainer
import com.terkula.uaxctf.statistics.response.RunnerMeetSplitResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.lang.Exception
import java.sql.Date

@Component
class MeetMileSplitService(@field:Autowired
                           private val meetMileSplitRepository: MeetMileSplitRepository,
                           @field:Autowired
                           private val meetRepository: MeetRepository,
                           @field:Autowired
                           private val runnerRepository: RunnerRepository,
                           @field:Autowired
                           private val meetPerformanceService: MeetPerformanceService) {


    fun getAllMeetMileSplitsForRunner(name: String, startDate: Date, endDate: Date): RunnerMeetSplitResponse {
        val meets = meetRepository.findByDateBetween(startDate, endDate)

        val runner = try {
            runnerRepository.findByNameContaining(name).first()
        } catch (e: Exception) {
            throw RunnerNotFoundByPartialNameException("np runner found by: $name")
        }

        val performances = meetPerformanceService.getMeetPerformancesForRunnerWithNameContaining(name,
                startDate, endDate, SortingMethodContainer.TIME, 20).map { it.performance }
                .flatten()
                .filter {
                    it.meetName in meets.map { meet -> meet.name }
                 }.map {
                    meetRepository.findByNameContainsAndDateBetween(it.meetName, startDate, endDate).first().id to it
                }.toMap()

        return RunnerMeetSplitResponse(runner, meets.map { it.id to meetMileSplitRepository.findByMeetIdAndRunnerId(it.id, runner.id).firstOrNull() }
                .filter { it.second != null }
                .map { it.first to it.second!!}
                .toMap()
                .map {
                    RunnerMeetSplitDTO(performances[it.key]!!, MeetSplitsDTO(it.value.mileOne, it.value.mileTwo, it.value.mileThree))
                })




    }


}