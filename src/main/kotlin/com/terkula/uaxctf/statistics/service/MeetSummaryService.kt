package com.terkula.uaxctf.statistics.service


import com.terkula.uaxctf.statistics.controller.MeetPerformanceController
import com.terkula.uaxctf.statistics.dto.FasterAndSlowerProgressions
import com.terkula.uaxctf.statistics.dto.ImprovementRatePair
import com.terkula.uaxctf.statistics.dto.SummaryImprovementRateDTO
import com.terkula.uaxctf.statistics.exception.MeetNotFoundException
import com.terkula.uaxctf.statisitcs.model.Meet
import com.terkula.uaxctf.statistics.repository.MeetRepository
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
                          internal var goalService: XcGoalService) {


    fun getLastMeetSummary(startSeasonDate: Date, endSeasonDate: Date, meetName: String): MeetSummaryResponse {

        var targetMeet: Meet
        try {
            targetMeet = meetRepository.findByNameContainsAndDateBetween(meetName, startSeasonDate, endSeasonDate).first()
        } catch (e: Exception) {
            throw MeetNotFoundException("unable to find meet by name: $meetName")
        }


        val seasonBests = seasonBestService.getSeasonBestsAtLastMeet(startSeasonDate, targetMeet.date)
        val seasonBestResponse = SeasonBestResponse(seasonBests.size, seasonBests)

        val prs = personalRecordService.getPRsAtLastMeet(startSeasonDate, targetMeet.date
        )
        val prResponse = PRResponse(prs.size, prs)


        val metGoalsResponse = MetGoalResponse(goalService.getRunnerWhoNewlyMetGoalAtMeet(meetName, startSeasonDate, endSeasonDate))

        val improvementRateDTOs = improvementRateService.getImprovementRatesAtGivenMeet(startSeasonDate, endSeasonDate, meetName)

        val faster = improvementRateDTOs.filter { it.improvementRate <= 0 }.toMutableList().sortedBy { it.improvementRate }
        val slower = improvementRateDTOs.filter { it.improvementRate > 0 }.toMutableList().sortedBy { it.improvementRate }

        val averageImprovementRate = (improvementRateDTOs.map { it.improvementRate }.sum()/improvementRateDTOs.size).round(2)

        val medianImprovementRate = improvementRateDTOs.map { it.improvementRate }.sorted()[(improvementRateDTOs.size/2)]

        val summaryImprovementRateDTO = SummaryImprovementRateDTO(averageImprovementRate.toMinuteSecondString(), medianImprovementRate.toMinuteSecondString(), ImprovementRatePair(faster.size, faster), ImprovementRatePair(slower.size, slower))


        var startDate = Date.valueOf((MeetPerformanceController.CURRENT_YEAR.toInt()-1).toString() + "-01-01")
        var endDate = Date.valueOf((MeetPerformanceController.CURRENT_YEAR) + "-12-31")


        val fasterThanLastYear =  meetProgressionService.getProgressionFromMeetForAllRunnersBetweenDates(meetName, startDate, endDate, "faster")
        val slowerThanLastYear = meetProgressionService.getProgressionFromMeetForAllRunnersBetweenDates(meetName, startDate, endDate, "slower")

        val fasterResponse = MeetProgressionResponse(fasterThanLastYear.size, fasterThanLastYear)
        val slowerResponse = MeetProgressionResponse(slowerThanLastYear.size, slowerThanLastYear)

        return MeetSummaryResponse(seasonBestResponse, prResponse, metGoalsResponse, summaryImprovementRateDTO, FasterAndSlowerProgressions(fasterResponse, slowerResponse))

    }

}