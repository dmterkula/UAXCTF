package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statistics.dto.MeetPerformanceDTO
import com.terkula.uaxctf.statistics.dto.MeetProgressionDTO
import com.terkula.uaxctf.statistics.dto.getTimeDifferencesAsDoubles
import com.terkula.uaxctf.statistics.dto.getTimeDifferencesAsStrings
import com.terkula.uaxctf.statistics.exception.MeetProgressionException
import com.terkula.uaxctf.statisitcs.model.toMeetPerformanceDTO
import com.terkula.uaxctf.statistics.repository.MeetPerformanceRepository
import com.terkula.uaxctf.statistics.repository.MeetRepository
import com.terkula.uaxctf.statistics.repository.RunnerRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.lang.Exception
import java.sql.Date

@Component
class MeetProgressionService(@field:Autowired
                             private val meetRepository: MeetRepository, @field:Autowired
                             private val meetPerformanceRepository: MeetPerformanceRepository,
                             @field:Autowired
                             private val runnerRepository: RunnerRepository) {



    fun getSingleMeetSingleRunnerProgression(meetName: String, runnerName: String): List<MeetProgressionDTO> {

        val meets = meetRepository.findByNameContains(meetName)

        val meetMap = meets.map { it.id to it }.toMap()

        val runner = runnerRepository.findByNameContaining(runnerName).first()

        val performancePairs = try {
            runner to meets.map{ meetPerformanceRepository.findByMeetIdAndRunnerId(it.id, runner.id) }
        } catch (e: Exception){
            throw MeetProgressionException("according to our records, $runnerName did not run at a meet matching: $meetName for multiple years")
        }

        performancePairs.second.toMutableList().sortBy {meetMap[it.meetId]!!.date }

        val performanceMap = listOf(runner to performancePairs.second.map {
            MeetPerformanceDTO(meetMap[it.meetId]!!.name, meetMap[it.meetId]!!.date,
                    it.time, it.place)
        })
                .map { MeetProgressionDTO(it.first, it.second, it.second.getTimeDifferencesAsStrings()) }
        return performanceMap

    }

    fun getProgressionFromMeetForAllRunnersBetweenDates(meetName: String, startDate: Date, endDate: Date, filterBy: String): List<MeetProgressionDTO> {

        val meets = meetRepository.findByNameContainsAndDateBetween(meetName, startDate, endDate)

        val meetMap = meets.map { it.id to it }.toMap()

        val runnerMap = runnerRepository.findAll().map {it.id to it}.toMap()

        val performances = meets.map { meetPerformanceRepository.findByMeetId(it.id)}
                .flatten()
                .groupBy { it.runnerId }
                .filter { it.value.size > 1}
                .map { runnerMap[it.key]!! to it.value.toMeetPerformanceDTO(meetMap) }.toMap()


        val runnerToTimeDifference = performances
                .map { it.key to it.value.getTimeDifferencesAsStrings() }
                .toMap()
                .toMutableMap()

        val progressionDTOs = performances.map { MeetProgressionDTO(it.key, it.value, runnerToTimeDifference[it.key]!!) }

        val fasterDTOs =  progressionDTOs.filter { it.progression.first().contains("-") }.sortedByDescending { it.progression.first() }
        val slowerDTOs =  progressionDTOs.filter { !it.progression.first().contains("-") }.sortedBy { it.progression.first() }

        return when {
            filterBy.isEmpty() -> fasterDTOs.plus(slowerDTOs)
            filterBy == "faster" -> fasterDTOs
            else -> slowerDTOs
        }

    }

    fun getMeetProgressionsFromLastNMeets(numMeets: Int, currentYear: String, startDate: Date, endDate: Date, filterBy: String,
                                          excludedMeet: String): List<MeetProgressionDTO> {

        // find all runners whose graduating class is > current year
        val eligibleRunners = runnerRepository.findByGraduatingClassGreaterThan(currentYear).map { it.id to it }.toMap()

        // find all performances for those runners this year

        val meets =
                if(excludedMeet.isEmpty()) {
                    meetRepository.findByDateBetween(startDate, endDate)
                            .toMutableList().sortedByDescending { it.date }
                } else {
                    meetRepository.findByDateBetween(startDate, endDate)
                            .filter{ !it.name.contains(excludedMeet, ignoreCase = true) }.toMutableList().sortedByDescending { it.date }
                }

        val meetMap = meets.map { it.id to it }.toMap()

        val meetProgressionDTOs = eligibleRunners.map { meetPerformanceRepository.findByRunnerId(it.key) }
                .flatten().groupBy { it.runnerId }
                .map {
                    it.key to it.value.
                            filter { perf -> perf.meetId in meetMap }
                            .toMutableList()
                            .sortedByDescending { perf -> meetMap[perf.meetId]!!.date }.take(numMeets)
                }.toMap()
                .map { it.key to it.value.toMeetPerformanceDTO(meetMap) }
                .map { MeetProgressionDTO(eligibleRunners[it.first]!!, it.second, it.second.getTimeDifferencesAsStrings()) }


        return when {
            filterBy.isEmpty() -> meetProgressionDTOs.filter { it.progression.all { time -> time.contains("-") } }.sortedBy {
                it.meetPerformanceDTOs.getTimeDifferencesAsDoubles().sum()}.plus(meetProgressionDTOs.filter { it.progression.all { time -> !time.contains("-") } }.toMutableList()
                    .sortedBy { it.meetPerformanceDTOs.getTimeDifferencesAsDoubles().sum() })

            filterBy == "faster" -> meetProgressionDTOs.filter { it.progression.all { time -> time.contains("-") } }.sortedBy {
                it.meetPerformanceDTOs.getTimeDifferencesAsDoubles().sum()
            }
            else -> meetProgressionDTOs.filter { it.progression.all { time -> !time.contains("-") } }
                    .sortedByDescending { it.meetPerformanceDTOs.getTimeDifferencesAsDoubles().sum() }
        }

    }

}
