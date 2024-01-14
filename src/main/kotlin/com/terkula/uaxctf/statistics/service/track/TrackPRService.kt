package com.terkula.uaxctf.statistics.service.track

import com.terkula.uaxctf.statisitcs.model.track.TrackMeet
import com.terkula.uaxctf.statisitcs.model.track.TrackMeetPerformance
import com.terkula.uaxctf.statisitcs.model.track.getLogicalEvent
import com.terkula.uaxctf.statisitcs.model.track.toTrackMeetPerformanceResponse
import com.terkula.uaxctf.statistics.dto.track.TrackTopResult
import com.terkula.uaxctf.statistics.dto.track.TrackPerformancesDTO
import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.statistics.repository.track.TrackMeetPerformanceRepository
import com.terkula.uaxctf.statistics.repository.track.TrackMeetRepository
import com.terkula.uaxctf.util.calculateSecondsFrom
import com.terkula.uaxctf.util.toMinuteSecondMillisString
import org.springframework.stereotype.Component
import java.sql.Date

@Component
class TrackPRService(
    private val meetRepository: TrackMeetRepository,
    private val meetPerformanceRepository: TrackMeetPerformanceRepository,
    private val runnerRepository: RunnerRepository
) {

    fun getARunnersPRs(runnerId: Int, includeSplits: Boolean, filterEvent: String, convertToMetric: Boolean): TrackPerformancesDTO {

        val meetMap = meetRepository.findAll().map { it.uuid to it }.toMap()
        val runner = runnerRepository.findById(runnerId)


        var results = meetPerformanceRepository.findByRunnerId(runnerId)
        if (!includeSplits) {
            results = results .filter { !it.isSplit }.toMutableList()
        }


        var prs: Map<String, List<TrackMeetPerformance>> = results
                .groupBy { it.getLogicalEvent(convertToMetric) }

        if (!filterEvent.isEmpty())   {
            prs = prs.filter { it.key == filterEvent.getLogicalEvent(convertToMetric) }
        }

        val prDTOs =
                prs
                .map { Triple(it.key, it.value, it.value.sortedBy { perf-> perf.time.calculateSecondsFrom() }.firstOrNull())
        }.filter { it.third != null }
                .map {
                    val previousBest = findPreviousBest(it.third, it.second, meetMap)
                    var timeDifference = "0:00.00"
                    if (previousBest != null) {
                        timeDifference = (previousBest.time.calculateSecondsFrom() - it.third!!.time.calculateSecondsFrom()).toMinuteSecondMillisString()
                    }
                TrackTopResult(it.first, meetMap[it.third!!.meetId]!!, it.third!!.toTrackMeetPerformanceResponse(meetMap[it.third!!.meetId]!!.name, meetMap[it.third!!.meetId]!!.date), previousBest?.toTrackMeetPerformanceResponse(meetMap[previousBest.meetId]!!.name, meetMap[previousBest.meetId]!!.date), timeDifference)
             }.sortedBy { it.event }

            return TrackPerformancesDTO(runner.get(), prDTOs)
        }

    fun getARunnersPRsAsOfDate(date: Date, runnerId: Int, includeSplits: Boolean, filterEvent: String): TrackPerformancesDTO {

        val meetMap = meetRepository.findAll().map { it.uuid to it }.toMap()
        val runner = runnerRepository.findById(runnerId)


        var results = meetPerformanceRepository.findByRunnerId(runnerId)
        if (!includeSplits) {
            results = results .filter { !it.isSplit }.toMutableList()
        }

        results = results.filter { meetMap[it.meetId]!!.date.before(date) || meetMap[it.meetId]!!.date == date }.toMutableList()

        var prs: Map<String, List<TrackMeetPerformance>> = results
                .groupBy { it.getLogicalEvent() }

        if (!filterEvent.isEmpty())   {
            prs = prs.filter { it.key == filterEvent.getLogicalEvent(false) }
        }

        val prDTOs =
                prs
                        .map { Triple(it.key, it.value, it.value.sortedBy { perf-> perf.time.calculateSecondsFrom() }.firstOrNull())
                        }.filter { it.third != null }
                        .map {
                            val previousBest = findPreviousBest(it.third, it.second, meetMap)
                            var timeDifference = "0:00.00"
                            if (previousBest != null) {
                                timeDifference = (previousBest.time.calculateSecondsFrom() - it.third!!.time.calculateSecondsFrom()).toMinuteSecondMillisString()
                            }
                            TrackTopResult(it.first, meetMap[it.third!!.meetId]!!, it.third!!.toTrackMeetPerformanceResponse(meetMap[it.third!!.meetId]!!.name, meetMap[it.third!!.meetId]!!.date), previousBest?.toTrackMeetPerformanceResponse(meetMap[previousBest.meetId]!!.name, meetMap[previousBest.meetId]!!.date), timeDifference)
                        }.sortedBy { it.event }

        return TrackPerformancesDTO(runner.get(), prDTOs)
    }

    fun findPreviousBest(
        pr: TrackMeetPerformance?,
        records: List<TrackMeetPerformance>,
        meets: Map<String, TrackMeet>
    ): TrackMeetPerformance? {

        if (pr == null) {
            return null
        }

        return records.filter { meets[it.meetId]!!.date.before(meets[pr.meetId]!!.date) }.sortedBy { it.time.calculateSecondsFrom() }
                .firstOrNull()
    }


    fun getAllPRsForWorkoutPlan(startingGradClass: String, includeSplits: Boolean, filterEvent: String): List<TrackPerformancesDTO> {

        var equiv1600 = listOf("1600", "1600m", "1609", "1609m", "Mile")
        var equiv3200 = listOf("3200", "3200m", "3218m", "3218", "2 Mile")

        // find all runners whose graduating class is > current year | find by given grad class
        val eligibleRunners = runnerRepository.findByGraduatingClassGreaterThanEqual(startingGradClass)

        return eligibleRunners.map {
            getARunnersPRs(it.id, includeSplits, filterEvent, true)
        }.sortedBy { it.bestResults.firstOrNull { pr -> pr.event == filterEvent }?.best?.time?.calculateSecondsFrom() }

    }

    fun getAllPRs(startingGradClass: String, includeSplits: Boolean, filterEvent: String) : List<TrackPerformancesDTO> {
        val eligibleRunners = runnerRepository.findByGraduatingClassGreaterThanEqual(startingGradClass)

        return eligibleRunners.map {
            getARunnersPRs(it.id, includeSplits, filterEvent, false)
        }.sortedBy { it.bestResults.firstOrNull { pr -> pr.event == filterEvent }?.best?.time?.calculateSecondsFrom() }
    }

    fun getAllPRsAsOfDate(date: Date, startingGradClass: String, includeSplits: Boolean, filterEvent: String) : List<TrackPerformancesDTO> {
        val eligibleRunners = runnerRepository.findByGraduatingClassGreaterThanEqualAndDoesTrack(startingGradClass, true)

        return eligibleRunners.map {
            getARunnersPRsAsOfDate(date, it.id, includeSplits, "")
        }.sortedBy { it.bestResults.firstOrNull { pr -> pr.event == filterEvent }?.best?.time?.calculateSecondsFrom() }
    }

//    fun getPRsByName(partialName: String): List<TrackPRsDTO?> {
//
//        val eligibleRunners = runnerRepository.findByNameContaining(partialName)
//                .map { it.id to it }.toMap()
//
//        val meets = meetRepository.findAll().toMutableList()
//
//        val meetMap = meets.map { it.id to it }.toMap()
//
//        val prDTOs = eligibleRunners.map { meetPerformanceRepository.findByRunnerId(it.key) }
//                .flatten().groupBy { it.runnerId }
//                .map {
//                    it.key to it.value.filter { perf -> perf.meetId in meetMap }
//                }.toMap()
//                .map { eligibleRunners[it.key]!! to it.value.toTrackMeetPerformanceDTO(meetMap) }
//                .filter { it.second.isNotEmpty() }
//                .groupAndSort(1)
//                .map {
//                    TrackPRsDTO(it.first, it.second)
//                }
//
//        return prDTOs
//
//    }

//    fun getPRsAtLastMeet(startDate: Date, endDate: Date): List<TrackPRsDTO> {
//
//        return getAllPRs(startDate.getYearString(), "", TrackSortingMethodContainer.TIME)
//                .map {
//                    val filteredMeetPerformances = it.pr.filter { mapEntry -> mapEntry.value.first().meetDate == endDate }
//                    TrackPRsDTO(it.runner, filteredMeetPerformances)
//                }.filter {
//                    it.pr.values.isNotEmpty()
//                }
//
//    }
}