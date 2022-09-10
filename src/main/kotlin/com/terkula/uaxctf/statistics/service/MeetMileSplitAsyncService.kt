package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statistics.dto.StatisticalComparisonDTO
import com.terkula.uaxctf.statistics.dto.TStatDTO
import com.terkula.uaxctf.statistics.repository.MeetMileSplitRepository
import com.terkula.uaxctf.statistics.repository.MeetRepository
import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.statistics.response.TTestResponse
import com.terkula.uaxctf.util.*
import org.apache.commons.math3.stat.inference.TestUtils
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.AsyncResult
import org.springframework.stereotype.Service
import java.sql.Date
import java.util.concurrent.Future

@Service
class MeetMileSplitAsyncService(
        private val meetRepository: MeetRepository,
        private val meetMileSplitRepository: MeetMileSplitRepository,
        private val seasonBestService: SeasonBestService,
        private val personalRecordService: PersonalRecordService,
        private val runnerRepository: RunnerRepository
        ) {

    @Async
    fun runTwoSampleTTestForMileSplitsForAllMeets(
            filterMeet: String,
            startDate1: Date,
            endDate1: Date,
            startDate2: Date,
            endDate2: Date,
            comparisonPace: String
    ): Future<TTestResponse> {
        return AsyncResult(runTwoSampleTTestForMileSplits(filterMeet, startDate1, endDate1, startDate2, endDate2, comparisonPace))
    }

    fun runTwoSampleTTestForMileSplits(
            filterMeet: String,
            startDate1: Date,
            endDate1: Date,
            startDate2: Date,
            endDate2: Date,
            comparisonPace: String
    ): TTestResponse {

        val dataYear1 = getMeetSplitsToComparision(filterMeet, comparisonPace, startDate1, endDate1)
        val dataYear2 = getMeetSplitsToComparision(filterMeet, comparisonPace, startDate2, endDate2)

        var labelPrefixYear1 = "$filterMeet ${startDate1.getYearString()}:"
        var labelPrefixYear2 = "$filterMeet ${startDate2.getYearString()}:"

        var label: String = if (comparisonPace.equals("SB", true)) {
            "percent of previous season best pace "
        } else {
            "percent of PR pace"
        }

        val mile1DistributionYear1 = StatisticalComparisonDTO.from("$labelPrefixYear1 mile 1 $label", dataYear1.map {it.first }, "decimal", 4)
        val mile2DistributionYear1 = StatisticalComparisonDTO.from("$labelPrefixYear1 mile 2 $label", dataYear1.map{ it.second }, "decimal", 4)
        val mile3DistributionYear1 = StatisticalComparisonDTO.from("$labelPrefixYear1 mile 3 $label", dataYear1.map{ it.third }, "decimal", 4)

        val mile1DistributionYear2 = StatisticalComparisonDTO.from("$labelPrefixYear2 mile 1 $label", dataYear2.map {it.first }, "decimal", 4)
        val mile2DistributionYear2 = StatisticalComparisonDTO.from("$labelPrefixYear2 mile 2 $label", dataYear2.map{ it.second }, "decimal", 4)
        val mile3DistributionYear2 = StatisticalComparisonDTO.from("$labelPrefixYear2 mile 3 $label", dataYear2.map{ it.third }, "decimal", 4)

        val tStatDTOMile1 = TStatDTO("t test comparing mile1 splits", TestUtils.tTest(dataYear1.map { it.first }.toDoubleArray(), dataYear2.map { it.first }.toDoubleArray()).round(4))
        val tStatDTOMile2 = TStatDTO("t test comparing mile2 splits", TestUtils.tTest(dataYear1.map{ it.second }.toDoubleArray(), dataYear2.map { it.second }.toDoubleArray()).round(4))
        val tStatDTOMile3 = TStatDTO("t test comparing mile3 splits", TestUtils.tTest(dataYear1.map{ it.third }.toDoubleArray(), dataYear2.map { it.third }.toDoubleArray()).round(4))


        return TTestResponse(
                listOf(mile1DistributionYear1, mile2DistributionYear1, mile3DistributionYear1),
                listOf(mile1DistributionYear2, mile2DistributionYear2, mile3DistributionYear2),
                listOf(tStatDTOMile1, tStatDTOMile2, tStatDTOMile3)
        )
    }

    fun getMeetSplitsToComparision(filterMeet: String,
                                   comparisonPace: String,
                                   startDate: Date,
                                   endDate: Date): List<Triple<Double, Double, Double>> {
        val meets = meetRepository.findByNameAndDateBetween(filterMeet, startDate, endDate)

        // all splits at meets in date range
        val splits = meets.
        map { meetMileSplitRepository.findByMeetId(it.id) }
                .filter { it.isNotEmpty() }
                .flatten()


        return if (comparisonPace.equals("SB", true)) {
            splits.map {
                runnerRepository.findById(it.runnerId).get() to it
            }.map {
                seasonBestService.getSeasonBestsByName(it.first.name, listOf(
                        TimeUtilities.getFirstDayOfGivenYear(startDate.subtractYear(1).getYearString()) to
                                TimeUtilities.getLastDayOfGivenYear(endDate.subtractYear(1).getYearString())),
                        false).firstOrNull() to it.second
            }
                    .filter { it.first != null }
                    .map { it.first!!.seasonBest to it.second}
                    .filter { it.first.isNotEmpty() }
                    .map { it.first.first().time.getPacePerMile() to it.second}
                    .map {
                        Triple(it.second.mileOne.calculateSecondsFrom() / it.first,
                                it.second.mileTwo.calculateSecondsFrom() / it.first,
                                it.second.mileThree.calculateSecondsFrom() / it.first)
                    }

        } else if (comparisonPace.equals("PR", true)) {
            splits.map {
                runnerRepository.findById(it.runnerId).get() to it
            }.map {
                personalRecordService.getPRsByNameBeforeTargetDate(it.first.name, false, meets[0].date.subtractDay()).firstOrNull() to it.second
            }
                    .filter { it.first != null }
                    .map { it.first!!.pr to it.second}
                    .filter { it.first.isNotEmpty() }
                    .map { it.first.first().time.getPacePerMile() to it.second}
                    .map {
                        Triple(it.second.mileOne.calculateSecondsFrom() / it.first,
                                it.second.mileTwo.calculateSecondsFrom() / it.first,
                                it.second.mileThree.calculateSecondsFrom() / it.first)
                    }

        } else {
            listOf(Triple(0.0, 0.0, 0.0))
        }
    }

}