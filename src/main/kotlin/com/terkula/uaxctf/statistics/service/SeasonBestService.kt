package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statistics.dto.ImprovedUponDTO
import com.terkula.uaxctf.statistics.dto.leaderboard.SeasonBestDTO
import com.terkula.uaxctf.statistics.dto.getTimeDifferencesAsStrings
import com.terkula.uaxctf.statistics.exception.RunnerNotFoundByPartialNameException
import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.statisitcs.model.XCMeetPerformance
import com.terkula.uaxctf.statisitcs.model.toMeetPerformanceDTO
import com.terkula.uaxctf.statistics.dto.MeetPerformanceDTO
import com.terkula.uaxctf.statistics.repository.MeetPerformanceRepository
import com.terkula.uaxctf.statistics.repository.MeetRepository
import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.statistics.repository.TimeTrialRepository
import com.terkula.uaxctf.util.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.sql.Date

@Component
class SeasonBestService(@field:Autowired
                        private val meetRepository: MeetRepository, @field:Autowired
                        private val meetPerformanceRepository: MeetPerformanceRepository,
                        @field:Autowired
                        private val runnerRepository: RunnerRepository,
                        @field:Autowired
                        private val timeTrialRepository: TimeTrialRepository,
                        @field: Autowired
                        private val performanceAdjusterService: PerformanceAdjusterService) {

    fun getAllSeasonBests(startDate: Date, endDate: Date, adjustForDistance: Boolean): List<SeasonBestDTO> {

        // get all meets in season/date range
        val meets = meetRepository.findByDateBetween(startDate, endDate)

        // look up map for meet id to meet
        val meetMap = meets.map { it.id to it }.toMap()

        // give all performances for the meets
        val performances = meets.map { meetPerformanceRepository.findByMeetId(it.id)}.flatten()

        // look up map for runner id to runner
        val runners = performances.map {
            it.runnerId to runnerRepository.findById(it.runnerId).get()
        }.toMap()

        val seasonBestDTOs = performances.groupBy { it.runnerId }
                .map { runners[it.key]!! to (it.value.toMutableList().sortedBy { performance -> performance.time }.take(2)) }.toMutableList()
                .map { it.first to performanceAdjusterService.adjustMeetPerformances(it.second.toMeetPerformanceDTO(meetMap), adjustForDistance) }.toMutableList()
                .sortedBy { it.second.first().time }
                .map {
                    val improvedUponMeetDTO = getImprovedUpon(it.second)
                    SeasonBestDTO(it.first, it.second.take(1), ImprovedUponDTO(it.second.getTimeDifferencesAsStrings()[0], improvedUponMeetDTO))
                }

        return seasonBestDTOs

    }

    fun getSeasonBestBeforeDate(runnerId: Int, date: Date): XCMeetPerformance? {

        val meets = meetRepository.findByDateBetween(TimeUtilities.getFirstDayOfGivenYear(date.getYearString()), date.subtractDay())
        val meetMap = meets.map { it.id to it }.toMap()
        return meets.map { meetPerformanceRepository.findByMeetIdAndRunnerId(it.id, runnerId) }
                .filterNotNull()
                .minByOrNull { it.time.calculateSecondsFrom() }

    }

    fun getSeasonBestTimeOrTrout(startDate: Date, endDate: Date, adjustForDistance: Boolean): List<Pair<Runner, String>> {
        val meets = meetRepository.findByDateBetween(startDate, endDate)

        // look up map for meet id to meet
        val meetMap = meets.map { it.id to it }.toMap()

        // give all performances for the meets
        val performances = meets.map { meetPerformanceRepository.findByMeetId(it.id)}.flatten()

        val runners = runnerRepository.findAll().map {
            it.id to it
        }.toMap()

        val tryouts: List<Pair<Runner, String>> = timeTrialRepository.findBySeason(startDate.getYearString()).map { runners[it.runnerId]!! to it.time.calculateSecondsFrom().scaleTimeTo5k(4827.0).toMinuteSecondString() }

        val seasonBests = performances.groupBy { it.runnerId }
                .map { runners[it.key]!! to (it.value.toMutableList().sortedBy { performance -> performance.time }.take(2)) }.toMutableList()
                .map { it.first to performanceAdjusterService.adjustMeetPerformances(it.second.toMeetPerformanceDTO(meetMap), adjustForDistance) }.toMutableList()
                .sortedBy { it.second.first().time }
                .map {
                    val improvedUponMeetDTO = getImprovedUpon(it.second)
                    SeasonBestDTO(it.first, it.second.take(1), ImprovedUponDTO(it.second.getTimeDifferencesAsStrings()[0], improvedUponMeetDTO))
                }
                .map {
                    it.runner.id to it
                }.toMap()


       val bestResult = tryouts.map {
           if(seasonBests[it.first.id] != null && seasonBests[it.first.id]!!.seasonBest.firstOrNull() != null && seasonBests[it.first.id]!!.seasonBest.first().time.calculateSecondsFrom() < it.second.calculateSecondsFrom()) {
               it.first to seasonBests[it.first.id]!!.seasonBest.first().time.calculateSecondsFrom().toMinuteSecondString()
           } else {
               it.first to it.second.calculateSecondsFrom().toMinuteSecondString()
           }
       }.sortedBy { it.second.calculateSecondsFrom() }

        return bestResult

    }

    fun getSeasonBestsByName(partialName: String, startEndDates: List<Pair<Date, Date>>, adjustForDistance: Boolean): List<SeasonBestDTO> {

        val runnersSeasonBests: MutableList<SeasonBestDTO> = mutableListOf()


        for (startEndDate: Pair<Date, Date> in startEndDates) {
            val meets = meetRepository.findByDateBetween(startEndDate.first, startEndDate.second)

            // look up map for meet id to meet
            val meetMap = meets.map { it.id to it }.toMap()

            // give all performances for the meets
            val performances = meets.map { meetPerformanceRepository.findByMeetId(it.id) }.flatten()

            // look up map for runner id to runner

            lateinit var runners: Map<Int, Runner>

            try {
                runners = listOf(runnerRepository.findByNameContaining(partialName).first()).map { it.id to it }.toMap()
            } catch (e: Exception) {
                throw RunnerNotFoundByPartialNameException("Unable to find results for runner by: $partialName")
            }

            val seasonBests = runners.map { runner -> performances.filter { it.runnerId == runner.key } }
                    .flatten()
                    .groupBy { it.runnerId }
                    .map {
                        it.key to it.value.toMutableList().sortedBy { performance -> performance.time }.take(2)
                    }.toMap()
                    .map {
                        it.key to performanceAdjusterService.adjustMeetPerformances(it.value.toMeetPerformanceDTO(meetMap), adjustForDistance)
                    }
                    .sortedBy { it.second.first().time }

                    .map {
                        val improvedUponMeetDTO = getImprovedUpon(it.second)
                        SeasonBestDTO(runners[it.first]!!, it.second.take(1), ImprovedUponDTO(it.second.getTimeDifferencesAsStrings()[0], improvedUponMeetDTO))
                    }

            if (seasonBests.isNotEmpty()) {
                runnersSeasonBests.add(seasonBests.first())
            }

        }

        return runnersSeasonBests

    }

    fun getSeasonBestsAtLastMeet(startDate: Date, endDate: Date, adjustForDistance: Boolean): List<SeasonBestDTO> {

        val latestMeet = meetRepository.findByDateBetween(startDate, endDate).sortedByDescending { it.date }.firstOrNull()
                ?: return emptyList()

        return getAllSeasonBests(startDate, endDate, false)
                .filter {
                    it.seasonBest.first().meetDate == latestMeet.date
                }
                .toMutableList()
                .sortedByDescending { it.improvedUpon.timeDifference }
    }

    fun findWhoseSeasonBestIsFirstMeet(startDate: Date, endDate: Date, adjustForDistance: Boolean): List<SeasonBestDTO> {

        // get all meets in season/date range
        val meets = meetRepository.findByDateBetween(startDate, endDate).sortedBy { it.date }

        return getAllSeasonBests(startDate, endDate, false).filter {
            it.seasonBest.first().meetDate == meets.first().date }.toMutableList().sortedByDescending { it.improvedUpon.timeDifference }
    }

}