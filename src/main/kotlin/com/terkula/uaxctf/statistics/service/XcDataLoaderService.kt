package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statisitcs.model.RaceResult
import com.terkula.uaxctf.statisitcs.model.XCMeetPerformance
import com.terkula.uaxctf.statistics.exception.MeetNotFoundException
import com.terkula.uaxctf.statistics.exception.MultipleMeetsFoundException
import com.terkula.uaxctf.statistics.exception.RunnerNotFoundException
import com.terkula.uaxctf.statistics.repository.MeetPerformanceRepository
import com.terkula.uaxctf.statistics.repository.MeetRepository
import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.statistics.response.MeetResultDataLoadResponse
import com.terkula.uaxctf.util.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class XcDataLoaderService(
        @field:Autowired val meetRepository: MeetRepository,
        @field:Autowired val runnerRepository: RunnerRepository,
        @field:Autowired val meetPerformanceRepository: MeetPerformanceRepository) {

    fun processRaceResults(results: List<List<Any>>, meetName: String): MeetResultDataLoadResponse {

        val startDate = TimeUtilities.getFirstDayOfYear()
        val endDate = TimeUtilities.getLastDayOfYear()

        val meet = meetRepository.findByNameAndDateBetween(meetName, startDate, endDate)
        val allMeets = meetRepository.findByDateBetween(startDate, endDate).map {it.name}

        if (meet.size == 0) {
            throw MeetNotFoundException("The sheet name/tab name provided does not match a meet name for the current year." +
            "meetName must match one of the following: " + allMeets.joinToString(","))
        } else if(meet.size > 1) {
            throw MultipleMeetsFoundException("more than one meet for this year matched that name")
        }

        var raceResults: List<RaceResult> = results.map {
            val data = it as List<String>
            return@map RaceResult(data[0], data[1].trim().appendDecimal(), data[2].toPlace())
        }

        val badInputs = raceResults
                .filter { !it.time.isValidTime() }
                .toMutableList()
                .plus(raceResults.filter { it.place == 0 })
                .distinctBy { it.name }

        raceResults = raceResults
                .filter { it.time.isValidTime() }
                .filter { it.place != 0 }

        val meetPerformances = raceResults.map {
            try {
                val runner = runnerRepository.findByName(it.name)
                return@map XCMeetPerformance(runner.id, meet.first().id, it.time, it.place)
            } catch (e: Exception) {
                throw RunnerNotFoundException("No runner found in the database matching the name: " + it.name)
            }
        }

        // delete existing results for same runner at same meet. This is the update case
        val existingResults = meetPerformanceRepository.findByMeetId(meet.first().id)

        val runnerIdsToInsert = meetPerformances.map { it.runnerId }

        meetPerformanceRepository.deleteAll(existingResults
                .filter { runnerIdsToInsert.contains(it.runnerId) }
        )

        meetPerformanceRepository.saveAll(meetPerformances)

        return MeetResultDataLoadResponse(raceResults, badInputs)
    }
}