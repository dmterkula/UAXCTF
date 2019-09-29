package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statistics.dto.MeetPerformanceDTO
import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.statisitcs.model.XCMeetPerformance
import com.terkula.uaxctf.statistics.repository.MeetPerformanceRepository
import com.terkula.uaxctf.statistics.repository.MeetRepository
import com.terkula.uaxctf.statistics.repository.RaceResultRepository
import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.statistics.request.SortingMethodContainer
import com.terkula.uaxctf.statistics.response.RunnerMeetPerformanceResponse
import java.sql.Date
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class MeetPerformanceService(@field:Autowired
                             private val meetRepository: MeetRepository, @field:Autowired
                             private val meetPerformanceRepository: MeetPerformanceRepository,
                             @field:Autowired
                             private val runnerRepository: RunnerRepository, @field:Autowired
                             private val raceResultRepository: RaceResultRepository) {

    fun loadMeetPerformance(meetId: Int) {
        val runners = runnerRepository.findAll().asSequence().toList()
        val raceResults = raceResultRepository.findAll().asSequence().toList()

        val xcMeetPerformances = raceResults.map { result ->
            val runnerList = runners.filter { runner: Runner -> runner.name.equals(result.name, ignoreCase = true) }
            if (runnerList.isEmpty()) {
                println("runner name in performance does not matcher roster: " + result.name)
            }

            return@map XCMeetPerformance(runnerList[0].id, meetId, result.time, result.place)
        }

        meetPerformanceRepository.saveAll(xcMeetPerformances)

    }

    fun getMeetPerformancesForRunnerWithNameContaining(partialName: String, startDate: Date, endDate: Date, sortingMethodContainer: SortingMethodContainer, count: Int): RunnerMeetPerformanceResponse {
        // find runners matching partial name
        val runnerIds = runnerRepository.findByNameContaining(partialName).map{ it.id }

        // get meets within date range
        val meets =  meetRepository.findByDateBetween(startDate, endDate)

        // construct map for meet id to meet
        val meetIdToMeetInfo = meets.map { it.id to it }.toMap()

        // construct all performances for the meets only for meets in date range, and containing an id of a matching runner
        val performances = meets.map { meetPerformanceRepository.findByMeetId(it.id)
                .filter { record -> runnerIds.contains(record.runnerId) }
        }.flatten()

        // construct map for runner id to runner over the selected performances
        val runners = performances.map {
            it.runnerId to runnerRepository.findById(it.runnerId).get()
        }.toMap()

        // group performances by id.
        // map runnerId to a MeetDTO constructed from performances and meet info map
        val performanceMap = performances.groupBy { it.runnerId }
                .map {
                    runners[it.key]!! to sortingMethodContainer.sortingFunction(it.value.map { meetPerformance ->
                        val meet = meetIdToMeetInfo[meetPerformance.meetId]!!
                        MeetPerformanceDTO(meet.name, meet.date, meetPerformance.time, meetPerformance.place)
                    }.toMutableList()).take(count)
                }.toMap()

        return RunnerMeetPerformanceResponse(performanceMap)

    }


    fun getMeetPerformancesAtMeetName(partialName: String, startDate: Date, endDate: Date, sortingMethodContainer: SortingMethodContainer, count: Int): RunnerMeetPerformanceResponse {
        // find runners matching partial name


        val meets =  meetRepository.findByNameContainsAndDateBetween(partialName, startDate, endDate)

        // construct map for meet id to meet
        val meetMap = meets.map { it.id to it }.toMap()


        // get meets within date range

        // construct all performances for the meets only for meets in date range, and containing an id of a matching runner
        val performances = meets.map { meetPerformanceRepository.findByMeetId(it.id)}.flatten()

        // construct map for runner id to runner over the selected performances
        val runners = performances.map {
            it.runnerId to runnerRepository.findById(it.runnerId).get()
        }.toMap()

        // group performances by id.
        // map runnerId to a MeetDTO constructed from performances and meet info map
        val performanceMap = performances.groupBy { it.runnerId }
                .map {
                    runners[it.key]!! to sortingMethodContainer.sortingFunction(it.value.map { meetPerformance ->
                        val meet = meetMap[meetPerformance.meetId]!!
                        MeetPerformanceDTO(meet.name, meet.date, meetPerformance.time, meetPerformance.place)
                    }.toMutableList()).take(count)
                }.toMap()

        return RunnerMeetPerformanceResponse(performanceMap)

    }




}

