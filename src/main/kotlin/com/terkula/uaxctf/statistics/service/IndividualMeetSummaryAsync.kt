package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.statisitcs.model.toMeetPerformanceDTO
import com.terkula.uaxctf.statistics.dto.MeetPerformanceDTO
import com.terkula.uaxctf.statistics.dto.RunnersMeetSplitsComparisonPaceDTO
import com.terkula.uaxctf.statistics.repository.MeetRepository
import com.terkula.uaxctf.statistics.request.SortingMethodContainer
import com.terkula.uaxctf.statistics.response.IndividualMeetSummaryResponse
import com.terkula.uaxctf.util.TimeUtilities
import com.terkula.uaxctf.util.calculateSecondsFrom
import com.terkula.uaxctf.util.getYearString
import com.terkula.uaxctf.util.round
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

@Service
class IndividualMeetSummaryAsync(
        val meetMileSplitService: MeetMileSplitService,
        val meetRepository: MeetRepository,
        val meetPerformanceService: MeetPerformanceService,
        val personalRecordService: PersonalRecordService,
        val seasonBestService: SeasonBestService
) {

    @Async
    fun getIndividualMeetSummary(runnerId: Int, meetUuid: String): Future<IndividualMeetSummaryResponse?> {

        val meet = meetRepository.findByUuid(meetUuid).first()
        val meets = meetRepository.findAll().map { it.id to it }.toMap()

        val allMeetResults = meetPerformanceService.getTotalMeetPerformancesAtMeet(meet.name, TimeUtilities.getFirstDayOfGivenYear(meet.date.getYearString()),
                TimeUtilities.getLastDayOfGivenYear(meet.date.getYearString()), SortingMethodContainer.NOSORT, 100, false, "UA")


        val result = allMeetResults.firstOrNull{ it.runner.id == runnerId } ?: return CompletableFuture.completedFuture(null)
        val runner = result.runner

        val allMeetSplits = meetMileSplitService.getAllMeetMileSplitsForRunner(runner.name, TimeUtilities.getFirstDayOfGivenYear("2017"), TimeUtilities.getLastDayOfGivenYear(meet.date.getYearString()))
                .mileSplits.filter { it.meetSplitsDTO != null }

        val meetSplitsInSeason = allMeetSplits.filter { it.meetPerformanceDTO.meetDate.getYearString() == meet.date.getYearString() }


        val pr = personalRecordService.getPRsForRunner(runnerId).minByOrNull { it.time.calculateSecondsFrom() }

        val isPrAtSameMeet: Boolean = pr?.meetDate == meet.date && pr.meetName == meet.name

        val seasonBest = seasonBestService.getAllSeasonBests(TimeUtilities.getFirstDayOfGivenYear(meet.date.getYearString()),
                TimeUtilities.getLastDayOfGivenYear(meet.date.getYearString()), false)
                .firstOrNull { it.runner.id == runnerId }

        val isSBAtSameMeet: Boolean = seasonBest?.seasonBest?.firstOrNull()?.meetDate == meet.date && seasonBest?.seasonBest.firstOrNull()?.meetName == meet.name

//        val pastPerformancesAtSameMeet =
//                meetPerformanceService.getMeetPerformancesForRunner(runnerId, TimeUtilities.getFirstDayOfGivenYear((runner.graduatingClass.toInt() - 4).toString()), TimeUtilities.getLastDayOfGivenYear(runner.graduatingClass))
//                        .filter { meets[it.meetId]?.name == meet.name }
//                        .sortedByDescending { meets[it.meetId]?.date }
//                        .drop(1)
//                        .map { it.toMeetPerformanceDTO(meets[it.meetId]!!) }
//
//        val sameSeasonMeetResults = meetPerformanceService.getMeetPerformancesForRunner(runnerId, TimeUtilities.getFirstDayOfGivenYear(meet.date.getYearString()),
//                TimeUtilities.getLastDayOfGivenYear(meet.date.getYearString()), SortingMethodContainer.RECENT_DATE, 50, false)


        // want milesplits vs previous PR pace.
        // want that analysis compared to the rest of the team

        val seasonPRSplitAnalysis = meetMileSplitService.getMeetSplitComparisonsForRunner(runner.name, TimeUtilities.getFirstDayOfGivenYear(meet.date.getYearString()),
                TimeUtilities.getLastDayOfGivenYear(meet.date.getYearString()), "pr")

        val prSplitAnalysisForMeet = seasonPRSplitAnalysis.meetSplits.filter { it.meet.uuid == meetUuid }

        val sbSplitAnalysis = meetMileSplitService.getMeetSplitComparisonsForRunner(runner.name, TimeUtilities.getFirstDayOfGivenYear(meet.date.getYearString()),
                TimeUtilities.getLastDayOfGivenYear(meet.date.getYearString()), "sb")

        val sbSplitAnalysisForMeet = sbSplitAnalysis.meetSplits.filter { it.meet.uuid == meetUuid }

        val baseTrainingPaceSplitAnalysis = meetMileSplitService.getMeetSplitComparisonsForRunner(runner.name, TimeUtilities.getFirstDayOfGivenYear(meet.date.getYearString()),
                TimeUtilities.getLastDayOfGivenYear(meet.date.getYearString()), "baseTraining")

        val baseTrainingPaceSplitAnalysisForMeet: RunnersMeetSplitsComparisonPaceDTO = baseTrainingPaceSplitAnalysis.meetSplits.filter { it.meet.uuid == meetUuid }
                .first()

        val allHistoricalMeetResultsSameMeet = meetPerformanceService.getMeetPerformancesAtMeetName(meet.name, TimeUtilities.getFirstDayOfGivenYear("2017"), TimeUtilities.getLastDayOfYear(), SortingMethodContainer.TIME, 1000, false)
                .filter { it.performance.isNotEmpty() }

        var flattenedHistoricalResults = mutableListOf<Pair<Runner, MeetPerformanceDTO>>()

        allHistoricalMeetResultsSameMeet.forEach { runnerResults ->
            runnerResults.performance.forEach {
                flattenedHistoricalResults.add(Pair(runnerResults.runner, it))
            }
        }

        flattenedHistoricalResults = flattenedHistoricalResults.sortedBy { it.second.time.calculateSecondsFrom() }.toMutableList()

        val meetRank = flattenedHistoricalResults.indexOfFirst { it.first == runner && it.second.meetDate == meet.date }


        val secondMileSlowDownOrder = allMeetResults.map { it.runner to (it.performance.mileTwoSplit.calculateSecondsFrom() - it.performance.mileOneSplit.calculateSecondsFrom())}.sortedBy { it.second }
        val thirdMileSlowDownOrder = allMeetResults.map { it.runner to (it.performance.mileThreeSplit.calculateSecondsFrom() - it.performance.mileTwoSplit.calculateSecondsFrom()) }.sortedBy { it.second }
        val spreadOrder = allMeetResults.map {
            val splitsList = listOf(it.performance.mileOneSplit.calculateSecondsFrom(), it.performance.mileTwoSplit.calculateSecondsFrom(), it.performance.mileThreeSplit.calculateSecondsFrom())
            val max = splitsList.maxOrNull()
            val min = splitsList.minOrNull()
            var spread = 0.0
            if (max != null && min != null) {
                spread = max - min
            }
            it.runner to spread
        }.sortedBy { it.second }

        val secondMileSlowDownRank = secondMileSlowDownOrder.indexOfFirst { it.first.id == runnerId } + 1
        val secondMileSlowDownPercentile: Double = (100 - ((secondMileSlowDownRank.toDouble() / secondMileSlowDownOrder.size) * 100)).round(2)

        val thirdMileSlowDownRank = thirdMileSlowDownOrder.indexOfFirst { it.first.id == runnerId } + 1
        val thirdMileSlowDownPercentile: Double = (100 - ((thirdMileSlowDownRank.toDouble() / thirdMileSlowDownOrder.size) * 100))

        val spreadRank = spreadOrder.indexOfFirst { it.first.id == runnerId } + 1
        val spreadRankPercentile: Double = (100 - ((spreadRank.toDouble() / spreadOrder.size) * 100)).round(2)

        val passesSecondMileVsFinishPlace = allMeetResults.map { it.runner to ((it.performance.passesSecondMile.toDouble() / it.performance.place) * 100).round(2)}.sortedByDescending { it.second }
        val passesThirdMileVsFinishPlace = allMeetResults.map { it.runner to ((it.performance.passesLastMile.toDouble() / it.performance.place) * 100).round(2)}.sortedByDescending { it.second }
        val totalPassesVsFinishPlace = allMeetResults.map { it.runner to ((it.performance.passesSecondMile + it.performance.passesLastMile).toDouble() / it.performance.place).round(2) }.sortedByDescending { it.second }

        val passesSecondMileVsFinishPosition: Double = passesSecondMileVsFinishPlace.filter{ it.first.id == runnerId }.first().second
        val passesSecondMileRank = passesSecondMileVsFinishPlace.indexOfFirst { it.first.id == runnerId } + 1
        val passesSecondMilePercentile = (100 - ((passesSecondMileRank.toDouble() / passesSecondMileVsFinishPlace.size) * 100)).round(2)

        val passesThirdMileVsFinishPosition: Double = passesThirdMileVsFinishPlace.filter{ it.first.id == runnerId }.first().second
        val passesThirdMileRank = passesThirdMileVsFinishPlace.indexOfFirst { it.first.id == runnerId } + 1
        val passesThirdMilePercentile = (100 - ((passesThirdMileRank.toDouble() / passesThirdMileVsFinishPlace.size) * 100)).round(2)

        val totalPassesVsFinishPosition: Double = totalPassesVsFinishPlace.filter{ it.first.id == runnerId }.first().second
        val totalPassesRank = totalPassesVsFinishPlace.indexOfFirst { it.first.id == runnerId } + 1
        val totalPassesPercentile = (100 - ((totalPassesRank.toDouble() / totalPassesVsFinishPlace.size) * 100)).round(2)

        val firstMileCareerRank: Int = allMeetSplits.sortedBy { it.meetSplitsDTO!!.mileOne.calculateSecondsFrom() }.indexOfFirst { it.meetPerformanceDTO.meetDate == meet.date && it.meetPerformanceDTO.meetName == meet.name }
        val firstMileSeasonRank: Int = meetSplitsInSeason.sortedBy { it.meetSplitsDTO!!.mileOne.calculateSecondsFrom() }.indexOfFirst { it.meetPerformanceDTO.meetDate == meet.date && it.meetPerformanceDTO.meetName == meet.name }

        val secondMileCareerRank: Int = allMeetSplits.sortedBy { it.meetSplitsDTO!!.mileTwo.calculateSecondsFrom() }.indexOfFirst { it.meetPerformanceDTO.meetDate == meet.date && it.meetPerformanceDTO.meetName == meet.name }
        val secondMileSeasonRank: Int = meetSplitsInSeason.sortedBy { it.meetSplitsDTO!!.mileTwo.calculateSecondsFrom() }.indexOfFirst { it.meetPerformanceDTO.meetDate == meet.date && it.meetPerformanceDTO.meetName == meet.name }

        val thirdMileCareerRank: Int = allMeetSplits.sortedBy { it.meetSplitsDTO!!.mileThree.calculateSecondsFrom() }.indexOfFirst { it.meetPerformanceDTO.meetDate == meet.date && it.meetPerformanceDTO.meetName == meet.name }
        val thirdMileSeasonRank: Int = meetSplitsInSeason.sortedBy { it.meetSplitsDTO!!.mileThree.calculateSecondsFrom() }.indexOfFirst { it.meetPerformanceDTO.meetDate == meet.date && it.meetPerformanceDTO.meetName == meet.name }


        return CompletableFuture.completedFuture(IndividualMeetSummaryResponse(
                result,
                runner,
                isPrAtSameMeet,
                isSBAtSameMeet,
                meetRank,
                secondMileSlowDownRank,
                secondMileSlowDownPercentile,
                thirdMileSlowDownRank,
                thirdMileSlowDownPercentile,
                spreadRank,
                spreadRankPercentile,
                passesSecondMileVsFinishPosition,
                passesThirdMileVsFinishPosition,
                passesSecondMileRank,
                passesThirdMileRank,
                passesSecondMilePercentile,
                passesThirdMilePercentile,
                totalPassesRank,
                totalPassesPercentile,
                totalPassesVsFinishPosition,
                sbSplitAnalysis.splitAverages,
                sbSplitAnalysisForMeet.firstOrNull(),
                baseTrainingPaceSplitAnalysis.splitAverages,
                baseTrainingPaceSplitAnalysisForMeet,
                firstMileCareerRank,
                secondMileCareerRank,
                thirdMileCareerRank,
                firstMileSeasonRank,
                secondMileSeasonRank,
                thirdMileSeasonRank
        )
        )


        // In the past, when someone has run a SB at a meet, what was the average split comparison % for each mile relative to SB pace.

    }
}