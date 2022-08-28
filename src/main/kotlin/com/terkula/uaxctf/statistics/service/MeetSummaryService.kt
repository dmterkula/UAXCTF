package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statistics.dto.FasterAndSlowerProgressions
import com.terkula.uaxctf.statistics.dto.ImprovementRatePair
import com.terkula.uaxctf.statistics.dto.SummaryImprovementRateDTO
import com.terkula.uaxctf.statistics.exception.MeetNotFoundException
import com.terkula.uaxctf.statisitcs.model.Meet
import com.terkula.uaxctf.statistics.repository.MeetRepository
import com.terkula.uaxctf.statistics.request.MeetSplitsOption
import com.terkula.uaxctf.statistics.response.*
import com.terkula.uaxctf.util.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.sql.Date

@Component
class MeetSummaryService (
    @field:Autowired
    internal var seasonBestService: SeasonBestService,
    @field:Autowired
    internal var personalRecordService: PersonalRecordService,
    @field: Autowired
    internal var improvementRateService: ImprovementRateService,
    @field: Autowired
    internal var meetProgressionService: MeetProgressionService,
    @field: Autowired
    internal var meetRepository: MeetRepository,
    @field: Autowired
    internal var goalService: XcGoalService,
    @field: Autowired
    internal var meetMileSplitService: MeetMileSplitService,
    @field: Autowired
    internal var meetSummaryAsyncHelper: MeetSummaryAsyncHelper
) {

    fun getLastMeetSummary(
        startSeasonDate: Date,
        endSeasonDate: Date,
        meetName: String,
        limit: Int,
        adjustForDistance: Boolean
    ): MeetSummaryResponse {

        val targetMeet: Meet
        try {
            targetMeet = meetRepository.findByNameAndDateBetween(meetName, startSeasonDate, endSeasonDate).first()
        } catch (e: Exception) {
            throw MeetNotFoundException("unable to find meet by name: $meetName")
        }

        // async ops

        val seasonBestsAsync = meetSummaryAsyncHelper.getSBs(startSeasonDate, targetMeet.date, adjustForDistance)

        val prsAsync = meetSummaryAsyncHelper.getPRs(startSeasonDate, targetMeet.date, adjustForDistance)

        val metGoals = meetSummaryAsyncHelper.getNewlyMetGoals(meetName, startSeasonDate, endSeasonDate, adjustForDistance)

        val meetSplitsStats = meetSummaryAsyncHelper.getStatisticsForMeet(meetName, startSeasonDate, endSeasonDate)

        val improvementRateDTOsFuture = meetSummaryAsyncHelper.getImprovementRatesAtGivenMeet(startSeasonDate, endSeasonDate, meetName)

        val fastestLastMileData = meetSummaryAsyncHelper.getMeetSplitInfo(meetName, MeetSplitsOption.SecondToThirdMile, startSeasonDate, endSeasonDate, "lowest", limit)

        val startDate = Date.valueOf(startSeasonDate.subtractYear(1).getYearString() + "-01-01")
        val endDate = Date.valueOf(startSeasonDate.getYearString() + "-12-31")

        val fasterThanLastYearFuture =
                meetSummaryAsyncHelper.getProgressionFromMeetForAllRunnersBetweenDates(meetName, startDate, endDate, "faster", adjustForDistance)
        val slowerThanLastYearFuture =
                meetSummaryAsyncHelper.getProgressionFromMeetForAllRunnersBetweenDates(meetName, startDate, endDate, "slower", adjustForDistance)


        // blocking calls to force complete async


        var seasonBests = seasonBestsAsync.get()
        val seasonBestResponse = SeasonBestResponse(seasonBests.size, seasonBests)

        var prs = prsAsync.get()
        val prResponse = PRResponse(prs.size, prs)

        val metGoalsResponse = MetGoalResponse(metGoals.get())

        val meetSplitStatisticResponse =
                MeetSplitsStatisticsSummaryResponse(meetSplitsStats.get())

        val fastestLastMileResponse = RunnerAvgSplitDifferenceResponse(fastestLastMileData.get())

        val fasterThanLastYear = fasterThanLastYearFuture.get()
        val slowerThanLastYear = slowerThanLastYearFuture.get()

        val fasterResponse = MeetProgressionResponse(fasterThanLastYear.size, fasterThanLastYear.take(limit))
        val slowerResponse = MeetProgressionResponse(slowerThanLastYear.size, slowerThanLastYear.take(limit))

        val improvementRateDTOs = improvementRateDTOsFuture.get()

        val faster = improvementRateDTOs.filter { it.improvementRate.calculateSecondsFrom() <= 0 }.toMutableList().sortedBy { it.improvementRate }
        val slower = improvementRateDTOs.filter { it.improvementRate.calculateSecondsFrom() > 0 }.toMutableList().sortedBy { it.improvementRate }

        val averageImprovementRate = (improvementRateDTOs.map { it.improvementRate.calculateSecondsFrom() }.sum()/improvementRateDTOs.size).round(2)
        val medianImprovementRate = improvementRateDTOs.map { it.improvementRate.calculateSecondsFrom() }.sorted()[(improvementRateDTOs.size / 2)]


        val summaryImprovementRateDTO = SummaryImprovementRateDTO(averageImprovementRate.toMinuteSecondString(),
                medianImprovementRate.toMinuteSecondString(), ImprovementRatePair(faster.size, faster.take(limit)),
                ImprovementRatePair(slower.size, slower.take(limit)))

        ////


        // finally construct response

        return MeetSummaryResponse(seasonBestResponse, prResponse, metGoalsResponse, meetSplitStatisticResponse,
                fastestLastMileResponse, summaryImprovementRateDTO, FasterAndSlowerProgressions(fasterResponse, slowerResponse))

    }
}