package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statisitcs.model.*
import com.terkula.uaxctf.statistics.exception.MeetNotFoundException
import com.terkula.uaxctf.statistics.exception.MultipleMeetsFoundException
import com.terkula.uaxctf.statistics.exception.RunnerNotFoundException
import com.terkula.uaxctf.statistics.repository.MeetMileSplitRepository
import com.terkula.uaxctf.statistics.repository.MeetPerformanceRepository
import com.terkula.uaxctf.statistics.repository.MeetRepository
import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.statistics.response.MeetResultDataLoadResponse
import com.terkula.uaxctf.statistics.response.MileSplitDataLoadResponse
import com.terkula.uaxctf.util.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class XcDataLoaderService(
        @field:Autowired val meetRepository: MeetRepository,
        @field:Autowired val runnerRepository: RunnerRepository,
        @field:Autowired val meetPerformanceRepository: MeetPerformanceRepository,
        @field:Autowired val meetMileSplitRepository: MeetMileSplitRepository) {

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

        var raceResults: List<RaceResult> = results
                .filter { list -> list.none { it.toString().isEmpty() } }
                .filter { list -> list.size > 2}
                .map {
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

     fun loadMileSplits(results: List<List<Any>>, meetName: String): MileSplitDataLoadResponse {

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

        val badInputs = results.map { it.map { value -> value.toString() } }
                .filter { list -> list.size != 5 }
                .toMutableList()

        badInputs.addAll(
                results.map { it.map { value -> value.toString() } }
                .filter { list -> list.size == 5 }.filter { list -> !list[1].isValidTime() || !list[3].isValidMileSplit() || !list[4].isValidMileSplit() }
        )

        val mileSplits = results
                .filter { list -> list.none { it.toString().isEmpty() } }
                .filter { list -> list.size == 5 }
                .filter { list -> list[1].toString().isValidTime() && list[3].toString().isValidMileSplit() && list[4].toString().isValidMileSplit() }
                .map {
                    val data = it as List<String>
                    try {
                        val runner = runnerRepository.findByName(it[0])
                        val split = parseSplits(it, meet.first().id, runner.id)
                        return@map split
                    } catch (e: Exception) {
                        throw RunnerNotFoundException("No runner found in the database matching " + it[0])
                    }
                }

        val existingMileSplits = meetMileSplitRepository.findByMeetId(meet.first().id)
         meetMileSplitRepository.deleteAll(existingMileSplits.filter { it.matches(existingMileSplits) })

        meetMileSplitRepository.saveAll(mileSplits)

        return MileSplitDataLoadResponse(mileSplits, badInputs)
    }

    private fun parseSplits(splits: List<String>, meetId: Int, runnerId: Int): MeetMileSplit {

        val mile3 = ((splits[1].calculateSecondsFrom() - splits[3].calculateSecondsFrom() - splits[4].calculateSecondsFrom()) / 1.1).toMinuteSecondString()

        return MeetMileSplit(meetId, runnerId, splits[3], splits[4], mile3)
    }

}