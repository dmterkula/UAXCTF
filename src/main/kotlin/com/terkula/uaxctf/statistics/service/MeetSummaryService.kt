package com.terkula.uaxctf.statistics.service


import com.terkula.uaxctf.statistics.controller.MeetPerformanceController
import com.terkula.uaxctf.statistics.dto.FasterAndSlowerProgressions
import com.terkula.uaxctf.statistics.dto.ImprovementRatePair
import com.terkula.uaxctf.statistics.dto.SummaryImprovementRateDTO
import com.terkula.uaxctf.statistics.exception.MeetNotFoundException
import com.terkula.uaxctf.statisitcs.model.Meet
import com.terkula.uaxctf.statistics.repository.MeetRepository
import com.terkula.uaxctf.statistics.request.MeetSplitsOption
import com.terkula.uaxctf.statistics.response.*
import com.terkula.uaxctf.util.round
import com.terkula.uaxctf.util.toMinuteSecondString
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.sql.Date

@Component
class MeetSummaryService (@field:Autowired
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
                          internal var meetMileSplitService: MeetMileSplitService) {


    fun getLastMeetSummary(startSeasonDate: Date, endSeasonDate: Date, meetName: String, limit: Int): MeetSummaryResponse {

        val targetMeet: Meet
        try {
            targetMeet = meetRepository.findByNameAndDateBetween(meetName, startSeasonDate, endSeasonDate).first()
        } catch (e: Exception) {
            throw MeetNotFoundException("unable to find meet by name: $meetName")
        }

        val seasonBests = seasonBestService.getSeasonBestsAtLastMeet(startSeasonDate, targetMeet.date)
        val seasonBestResponse = SeasonBestResponse(seasonBests.size, seasonBests)

        val prs = personalRecordService.getPRsAtLastMeet(startSeasonDate, targetMeet.date)
        val prResponse = PRResponse(prs.size, prs)


        val metGoalsResponse = MetGoalResponse(goalService.getRunnerWhoNewlyMetGoalAtMeet(meetName, startSeasonDate, endSeasonDate))

        val meetSplitStatisticResponse =  MeetSplitsStatisticsSummaryResponse(meetMileSplitService.getStatisticsForMeet(meetName, startSeasonDate, endSeasonDate))


        val improvementRateDTOs = improvementRateService.getImprovementRatesAtGivenMeet(startSeasonDate, endSeasonDate, meetName)

        val faster = improvementRateDTOs.filter { it.improvementRate <= 0 }.toMutableList().sortedBy { it.improvementRate }
        val slower = improvementRateDTOs.filter { it.improvementRate > 0 }.toMutableList().sortedBy { it.improvementRate }

        val averageImprovementRate = (improvementRateDTOs.map { it.improvementRate }.sum()/improvementRateDTOs.size).round(2)
        val medianImprovementRate = improvementRateDTOs.map { it.improvementRate }.sorted()[(improvementRateDTOs.size / 2)]


        val summaryImprovementRateDTO = SummaryImprovementRateDTO(averageImprovementRate.toMinuteSecondString(), medianImprovementRate.toMinuteSecondString(),
                ImprovementRatePair(faster.size, faster.take(limit)), ImprovementRatePair(slower.size, slower.take(limit)))

        val fastestLastMileResponse = RunnerAvgSplitDifferenceResponse(meetMileSplitService.getMeetSplitInfo(
                meetName, MeetSplitsOption.SecondToThirdMile, startSeasonDate, endSeasonDate, "lowest", limit))

        val startDate = Date.valueOf((MeetPerformanceController.CURRENT_YEAR.toInt()-1).toString() + "-01-01")
        val endDate = Date.valueOf((MeetPerformanceController.CURRENT_YEAR) + "-12-31")


        val fasterThanLastYear =  meetProgressionService.getProgressionFromMeetForAllRunnersBetweenDates(meetName, startDate, endDate, "faster")
        val slowerThanLastYear = meetProgressionService.getProgressionFromMeetForAllRunnersBetweenDates(meetName, startDate, endDate, "slower")

        val fasterResponse = MeetProgressionResponse(fasterThanLastYear.size, fasterThanLastYear.take(limit))
        val slowerResponse = MeetProgressionResponse(slowerThanLastYear.size, slowerThanLastYear.take(limit))

        return MeetSummaryResponse(seasonBestResponse, prResponse, metGoalsResponse, meetSplitStatisticResponse, fastestLastMileResponse, summaryImprovementRateDTO, FasterAndSlowerProgressions(fasterResponse, slowerResponse))

    }

}