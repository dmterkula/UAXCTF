package com.terkula.uaxctf.statistics.service.track

import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.statisitcs.model.toMeetPerformanceDTO
import com.terkula.uaxctf.statisitcs.model.track.TrackMeet
import com.terkula.uaxctf.statisitcs.model.track.TrackMeetPerformance
import com.terkula.uaxctf.statisitcs.model.track.getLogicalEvent
import com.terkula.uaxctf.statisitcs.model.track.toTrackMeetPerformanceResponse
import com.terkula.uaxctf.statistics.dto.track.TrackTopResult
import com.terkula.uaxctf.statistics.dto.track.TrackPerformancesDTO
import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.statistics.repository.track.TrackMeetPerformanceRepository
import com.terkula.uaxctf.statistics.repository.track.TrackMeetRepository
import com.terkula.uaxctf.util.TimeUtilities
import com.terkula.uaxctf.util.calculateSecondsFrom
import com.terkula.uaxctf.util.toMinuteSecondMillisString
import org.springframework.stereotype.Service
import java.sql.Date

@Service
class TrackSBService(
        private val meetRepository: TrackMeetRepository,
        private val meetPerformanceRepository: TrackMeetPerformanceRepository,
        private val runnerRepository: RunnerRepository
) {


    fun getARunnersSBs(runnerId: Int, includeSplits: Boolean, filterEvent: String, season: String): TrackPerformancesDTO {

        val startDate = TimeUtilities.getFirstDayOfGivenYear(season)
        val endDate = TimeUtilities.getLastDayOfGivenYear(season)


        val meetMap = meetRepository.findByDateBetween(startDate, endDate).map { it.uuid to it }.toMap()
        val runner = runnerRepository.findById(runnerId)

        var results = meetPerformanceRepository.findByRunnerId(runnerId)
                .filter { meetMap.containsKey(it.meetId) }

        if (!includeSplits) {
            results = results .filter { !it.isSplit }.toMutableList()
        }

        var sbs: Map<String, List<TrackMeetPerformance>> = results
                .groupBy { it.getLogicalEvent() }

        if (!filterEvent.isEmpty())   {
            sbs = sbs.filter { it.key == filterEvent.getLogicalEvent() }
        }

        val sbDTOs =
                sbs
                        .map { Triple(it.key, it.value, it.value.sortedBy { perf-> perf.time.calculateSecondsFrom() }.firstOrNull())
                        }.filter { it.third != null }
                        .map {
                            val previousBest = findPreviousBest(it.third, it.second, meetMap)
                            var timeDifference = "0:00.00"
                            if (previousBest != null) {
                                timeDifference = (previousBest.time.calculateSecondsFrom() - it.third!!.time.calculateSecondsFrom()).toMinuteSecondMillisString()
                            }
                            TrackTopResult(it.first, meetMap[it.third!!.meetId]!!, it.third!!.toTrackMeetPerformanceResponse(), previousBest?.toTrackMeetPerformanceResponse(), timeDifference)
                        }.sortedBy { it.event }

        return TrackPerformancesDTO(runner.get(), sbDTOs)
    }

    fun findPreviousBest(
            bestResult: TrackMeetPerformance?,
            records: List<TrackMeetPerformance>,
            meets: Map<String, TrackMeet>
    ): TrackMeetPerformance? {

        if (bestResult == null) {
            return null
        }

        return records.filter { meets[it.meetId]!!.date.before(meets[bestResult.meetId]!!.date) }.sortedBy { it.time.calculateSecondsFrom() }
                .firstOrNull()
    }


    fun getAllSBs(includeSplits: Boolean, filterEvent: String, season: String): List<TrackPerformancesDTO> {

        // find all runners whose graduating class is > current year | find by given grad class

        val eligibleRunners = runnerRepository.findByGraduatingClassGreaterThanEqual(season)

        return eligibleRunners.map {
            getARunnersSBs(it.id, includeSplits, filterEvent, season)
        }.sortedBy { it.bestResults.firstOrNull { pr -> pr.event == filterEvent }?.best?.time?.calculateSecondsFrom() }

    }

    fun getSeasonAverages(eligibleRunners: Map<Int, Runner>, event: String, startDate: Date, endDate: Date): Map<Int, List<Double>> {

        val meets = meetRepository.findByDateBetween(startDate, endDate)
                .toMutableList().sortedByDescending { it.date }

        val meetMap = meets.map { it.uuid to it }.toMap()

        return eligibleRunners.map { meetPerformanceRepository.findByRunnerIdAndEvent(it.key, event) }
                .flatten().groupBy { it.runnerId }
                .map {
                    it.key to it.value.
                    filter { perf -> perf.meetId in meetMap }
                            .toMutableList()
                            .sortedByDescending { perf -> meetMap[perf.meetId]!!.date }
                }.toMap()
                .filter { it.value.isNotEmpty() }
                .map {
                    val numMeets = it.value.size
                    it.key to listOf(it.value.map { perf -> perf.time.calculateSecondsFrom() }.sum()/numMeets)
                }.toMutableList().sortedBy { it.second.first() }.toMap()
    }




}