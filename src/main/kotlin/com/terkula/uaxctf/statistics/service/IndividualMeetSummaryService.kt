package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statistics.repository.MeetRepository
import com.terkula.uaxctf.statistics.request.SortingMethodContainer
import com.terkula.uaxctf.statistics.response.IndividualMeetSummaryResponse
import com.terkula.uaxctf.util.*
import org.springframework.stereotype.Service
import java.util.concurrent.Future

@Service
class IndividualMeetSummaryService(
        val meetRepository: MeetRepository,
        val meetPerformanceService: MeetPerformanceService,
        val individualMeetSummaryAsync: IndividualMeetSummaryAsync

        ) {

    fun getIndividualMeetSummaries(meetUuid: String): List<IndividualMeetSummaryResponse> {
        val meet = meetRepository.findByUuid(meetUuid).first()
        val allMeetResults = meetPerformanceService.getTotalMeetPerformancesAtMeet(meet.name, TimeUtilities.getFirstDayOfGivenYear(meet.date.getYearString()),
                TimeUtilities.getLastDayOfGivenYear(meet.date.getYearString()), SortingMethodContainer.NOSORT, 100, false, "UA")


        val futures: List<Future<IndividualMeetSummaryResponse?>> = allMeetResults.map {
            individualMeetSummaryAsync.getIndividualMeetSummary(it.runner.id, meetUuid)
        }

        return futures.mapNotNull { it.get() }

    }



}