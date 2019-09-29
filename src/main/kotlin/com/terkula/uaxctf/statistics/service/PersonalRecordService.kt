package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statistics.controller.MeetPerformanceController
import com.terkula.uaxctf.statistics.dto.*
import com.terkula.uaxctf.statisitcs.model.toMeetPerformanceDTO
import com.terkula.uaxctf.statistics.repository.MeetPerformanceRepository
import com.terkula.uaxctf.statistics.repository.MeetRepository
import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.statistics.request.SortingMethodContainer
import com.terkula.uaxctf.statistics.dto.ImprovedUponDTO
import com.terkula.uaxctf.statistics.dto.MeetProgressionDTO
import com.terkula.uaxctf.statistics.dto.PRDTO
import com.terkula.uaxctf.statistics.dto.getTimeDifferencesAsStrings
import com.terkula.uaxctf.util.getImprovedUpon
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.sql.Date

@Component
class PersonalRecordService(@field:Autowired
                            private val meetRepository: MeetRepository, @field:Autowired
                            private val meetPerformanceRepository: MeetPerformanceRepository,
                            @field:Autowired
                            private val runnerRepository: RunnerRepository) {

    fun getAllPRs(startingGradClass: String, filterClass: String, sortingMethodContainer: SortingMethodContainer): List<PRDTO> {

        //todo this will make a query for every eligible runner... consider sorting in the db in performances based on
        // todo time if pulling back all performances (per runner) is too expensive (this way you only pull back first 2)

        // find all runners whose graduating class is > current year | find by given grad class
        val eligibleRunners = if (filterClass.isNotEmpty()) {
            runnerRepository.findByGraduatingClass(filterClass)
        } else {
            runnerRepository.findByGraduatingClassGreaterThan(startingGradClass)
        }.map { it.id to it }.toMap()

        // find all performances for those runners this year

        val meets = meetRepository.findAll().toMutableList()

        val meetMap = meets.map { it.id to it }.toMap()

        var prDTOs = eligibleRunners.map { meetPerformanceRepository.findByRunnerId(it.key) }
                .flatten().groupBy { it.runnerId }
                .map {
                    it.key to it.value.filter { perf -> perf.meetId in meetMap }
                            .toMutableList()
                            .sortedBy { perf -> perf.time }.take(2)
                }.toMap()
                .map { it.key to it.value.toMeetPerformanceDTO(meetMap) }
                .filter { it.second.isNotEmpty() }
                .toMutableList()
                .sortedBy {
                    it.second.first().time
                }
                .map {
                    MeetProgressionDTO(eligibleRunners[it.first]!!, it.second, it.second.getTimeDifferencesAsStrings())
                }
                .map {
                    val improvedUponMeetDTO = getImprovedUpon(it.meetPerformanceDTOs)
                    PRDTO(it.runner, it.meetPerformanceDTOs.take(1), ImprovedUponDTO(it.meetPerformanceDTOs.getTimeDifferencesAsStrings()[0], improvedUponMeetDTO))
                }

        if (sortingMethodContainer.value == SortingMethodContainer.OLDER_DATE.value) {
            prDTOs = prDTOs.sortedBy { it.pr.first().meetDate }
        } else if (sortingMethodContainer.value == SortingMethodContainer.RECENT_DATE.value) {
            prDTOs = prDTOs.sortedByDescending { it.pr.first().meetDate }
        }

        return prDTOs
    }

    fun getPRsByName(partialName: String): List<PRDTO> {

        val eligibleRunners = runnerRepository.findByNameContaining(partialName)
                .map { it.id to it }.toMap()

        // find all performances for those runners this year

        val meets = meetRepository.findAll().toMutableList()

        val meetMap = meets.map { it.id to it }.toMap()

        val prDTOs = eligibleRunners.map { meetPerformanceRepository.findByRunnerId(it.key) }
                .flatten().groupBy { it.runnerId }
                .map {
                    it.key to it.value.filter { perf -> perf.meetId in meetMap }
                            .toMutableList()
                            .sortedBy { perf -> perf.time }.take(2)
                }.toMap()
                .map { it.key to it.value.toMeetPerformanceDTO(meetMap) }
                .filter { it.second.isNotEmpty() }
                .toMutableList()
                .sortedBy { it.second.first().time }
                .map { MeetProgressionDTO(eligibleRunners[it.first]!!, it.second, it.second.getTimeDifferencesAsStrings()) }
                .map {
                    val improvedUponMeetDTO = getImprovedUpon(it.meetPerformanceDTOs)

                    PRDTO(it.runner, it.meetPerformanceDTOs.take(1), ImprovedUponDTO(it.meetPerformanceDTOs.getTimeDifferencesAsStrings()[0], improvedUponMeetDTO))
                }


        return prDTOs

    }

    fun getPRsAtLastMeet(startDate: Date, endDate: Date): List<PRDTO> {

        val latestMeet = meetRepository.findByDateBetween(startDate, endDate).sortedByDescending { it.date }.first()

        return getAllPRs(MeetPerformanceController.CURRENT_YEAR, "", SortingMethodContainer.TIME)
                .filter {
                    it.pr.first().meetDate == latestMeet.date
                }
                .toMutableList()
                .sortedByDescending { it.improvedUpon.timeDifference }
    }



}