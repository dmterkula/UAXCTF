package com.terkula.uaxctf.statistics.service.track

//import com.terkula.uaxctf.statisitcs.model.track.toTrackMeetPerformanceDTO
import com.terkula.uaxctf.statistics.dto.track.TrackPRsDTO
import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.statistics.repository.track.TrackMeetPerformanceRepository
import com.terkula.uaxctf.statistics.repository.track.TrackMeetRepository
import com.terkula.uaxctf.statistics.response.track.TrackSortingMethodContainer

import com.terkula.uaxctf.util.getYearString
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.sql.Date

@Component
class TrackPRService(@field:Autowired
                            private val meetRepository: TrackMeetRepository, @field:Autowired
                            private val meetPerformanceRepository: TrackMeetPerformanceRepository,
                     @field:Autowired
                            private val runnerRepository: RunnerRepository) {

//    fun getAllPRs(startingGradClass: String, filterClass: String, sortingMethodContainer: TrackSortingMethodContainer): List<TrackPRsDTO> {
//
//        // find all runners whose graduating class is > current year | find by given grad class
//        val eligibleRunners = if (filterClass.isNotEmpty()) {
//            runnerRepository.findByGraduatingClass(filterClass)
//        } else {
//            runnerRepository.findByGraduatingClassGreaterThanEqual(startingGradClass)
//        }.map { it.id to it }.toMap()
//
//        // find all performances for those runners this year
//
//        val meets = meetRepository.findAll().toMutableList()
//
//        val meetMap = meets.map { it.id to it }.toMap()
//
//        return eligibleRunners.map { meetPerformanceRepository.findByRunnerId(it.key) }
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
//    }

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