package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statistics.exception.RunnerNotFoundByPartialNameException
import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.statisitcs.model.toMeetPerformanceDTO
import com.terkula.uaxctf.statistics.repository.MeetPerformanceRepository
import com.terkula.uaxctf.statistics.repository.MeetRepository
import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.statistics.dto.ImprovementRateDTO
import com.terkula.uaxctf.util.calculateSecondsFrom
import com.terkula.uaxctf.util.round
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.sql.Date

@Component
class ImprovementRateService (@field:Autowired
                              private val meetRepository: MeetRepository, @field:Autowired
                              private val meetPerformanceRepository: MeetPerformanceRepository,
                              @field:Autowired
                              private val runnerRepository: RunnerRepository) {

    fun getImprovementRateForRunner(runnerName: String, seasonStartDate: Date, seasonEndDate: Date, excludedMeet: String): List<ImprovementRateDTO> {

        var meets = meetRepository.findByDateBetween(seasonStartDate, seasonEndDate)

        // filters out meet by name within a particular season. if not for date range, might filter out same meet difference year
        if (excludedMeet.isNotEmpty()) {
            meets = meets.filter { !it.name.contains(excludedMeet) }.toMutableList()
        }

        // look up map for meet id to meet
        val meetMap = meets.map { it.id to it }.toMap()

        lateinit var runner: Runner
        try {
            runner = runnerRepository.findByNameContaining(runnerName).first()
        } catch (e: Exception) {
            throw RunnerNotFoundByPartialNameException("Unable to find results for runner by: $runnerName")
        }

        // give all performances for the meets
        val performances = meetPerformanceRepository.findByRunnerId(runner.id)
                .filter { it.meetId in meets.map { meet -> meet.id } }


        // look up map for runner id to runner

        val rateOfImprovement = performances.map {
            listOf(it).toMeetPerformanceDTO(meetMap).toMutableList().sortedBy { perf -> perf.meetDate }
        }.map {
            it.map { perf -> perf.time.calculateSecondsFrom() }
        }.flatten().calculateAverageRateOfImprovement()


        return listOf(ImprovementRateDTO(runner, rateOfImprovement.first, rateOfImprovement.second))
    }


    fun getImprovementRateForAllRunners(seasonStartDate: Date, seasonEndDate: Date, excludedMeet: String): List<ImprovementRateDTO> {

        // get all meets in season/date range
        var meets = meetRepository.findByDateBetween(seasonStartDate, seasonEndDate)

        // filters out meet by name within a particular season. if not for date range, might filter out same meet difference year
        if (excludedMeet.isNotEmpty()) {
            meets = meets.filter { !it.name.contains(excludedMeet, ignoreCase = true) }.toMutableList()
        }

        // look up map for meet id to meet
        val meetMap = meets.map { it.id to it }.toMap()

        // give all performances for the meets
        val performances = meets.map { meetPerformanceRepository.findByMeetId(it.id) }.flatten()

        // look up map for runner id to runner
        var runners = performances.map { it.runnerId to runnerRepository.findById(it.runnerId).get() }.toMap()


        val rateOfImprovements = performances.groupBy { it.runnerId }
                .map {
                    runners[it.key]!! to it.value.toMeetPerformanceDTO(meetMap).toMutableList().sortedBy { perf -> perf.meetDate }
                }.map {
                    it.first to it.second.map { perf -> perf.time.calculateSecondsFrom() }.calculateAverageRateOfImprovement()
                }.toMap().map {
                    ImprovementRateDTO(it.key, it.value.first, it.value.second)
                }.toMutableList().sortedBy { it.improvementRate }

        return rateOfImprovements
    }


    fun getImprovementRatesAtGivenMeet(seasonStartDate: Date, seasonEndDate: Date, givenMeet: String): List<ImprovementRateDTO> {


        val summaryMeet = meetRepository.findByNameAndDateBetween(givenMeet, seasonStartDate, seasonEndDate).first()

        // get all meets in season/date range
        var meets = meetRepository.findByDateBetween(seasonStartDate, seasonEndDate).toMutableList().sortedBy { it.date }
        meets = meets.subList(0, meets.indexOf(summaryMeet) + 1).toMutableList().sortedByDescending { it.date }.toMutableList()


        // filters out meet by name within a particular season. if not for date range, might filter out same meet difference year

        // look up map for meet id to meet
        val meetMap = meets.map { it.id to it }.toMap()

        // give all performances for the meets
        val performances = meets.map { meetPerformanceRepository.findByMeetId(it.id) }.flatten()

        // look up map for runner id to runner
        var runners = performances.map { it.runnerId to runnerRepository.findById(it.runnerId).get() }.toMap()


        val rateOfImprovements = performances.groupBy { it.runnerId }
                .filter { it.value.any { it.meetId == summaryMeet.id } } // give only runners who ran at the targetedMeet
                .map {
                    runners[it.key]!! to it.value.toMeetPerformanceDTO(meetMap).toMutableList().sortedBy { perf -> perf.meetDate }.takeLast(2)
                    // take the last 2 :) and it should work
                    // question is now how to make this more generic for between any meets instead of always just last two? filter

                }.map {
                    it.first to it.second.map { perf -> perf.time.calculateSecondsFrom() }.calculateAverageRateOfImprovement()
                }.toMap().map {
                    ImprovementRateDTO(it.key, it.value.first, it.value.second)
                }.toMutableList().sortedBy { it.improvementRate }

        return rateOfImprovements
    }

    fun List<Double>.calculateAverageRateOfImprovement(): Pair<Double, Int> {

        var rollingRate = 0.0

        if (this.size < 2) {
            return Pair(rollingRate, this.size)
        } else {

            for (i in 0..this.size) {
                if (i < this.size - 1) {
                    val priorMeetTime = this[i]
                    val nextMeetTime = this[i + 1]

                    val rawDifference = (nextMeetTime - priorMeetTime)
                    rollingRate += rawDifference

                }
            }

            val rate = (rollingRate / (this.size - 1)).round(2)

            return Pair(rate, this.size)

        }
    }
}
