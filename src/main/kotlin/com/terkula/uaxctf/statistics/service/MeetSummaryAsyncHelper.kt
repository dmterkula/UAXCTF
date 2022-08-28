package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statisitcs.model.NamedStatistic
import com.terkula.uaxctf.statistics.dto.*
import com.terkula.uaxctf.statistics.repository.MeetRepository
import com.terkula.uaxctf.statistics.request.MeetSplitsOption
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.AsyncResult
import org.springframework.stereotype.Component
import java.sql.Date
import java.util.concurrent.Future

@Component
class MeetSummaryAsyncHelper(
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
    internal var meetMileSplitService: MeetMileSplitService
  ) {



    @Async
    fun getPRs(startSeasonDate: Date, targetMeetDate: Date, adjustForDistance: Boolean): Future<List<PRDTO>> {
        return AsyncResult(personalRecordService.getPRsAtLastMeet(startSeasonDate, targetMeetDate, adjustForDistance))
    }

    @Async
    fun getSBs(startSeasonDate: Date, targetMeetDate: Date, adjustForDistance: Boolean): Future<List<SeasonBestDTO>> {
        return AsyncResult(seasonBestService.getSeasonBestsAtLastMeet(startSeasonDate, targetMeetDate, adjustForDistance))
    }

    @Async
    fun getNewlyMetGoals(
        meetName: String,
        startSeasonDate: Date,
        endSeasonDate: Date,
        adjustForDistance: Boolean
    ): Future<List<RunnersMetGoals>> {
        return AsyncResult(goalService.getRunnerWhoNewlyMetGoalAtMeet(meetName, startSeasonDate, endSeasonDate, adjustForDistance))
    }

    @Async
    fun getStatisticsForMeet(filterMeet: String, startDate: Date, endDate: Date): Future<List<NamedStatistic>> {
        return AsyncResult(meetMileSplitService.getStatisticsForMeet(filterMeet, startDate, endDate))
    }

    @Async
    fun getMeetSplitInfo(filterMeet: String, splitType: MeetSplitsOption, startDate: Date, endDate: Date, sort: String,
                         limit: Int): Future<List<RunnerAvgMileSplitDifferenceDTO>> {

        return AsyncResult(meetMileSplitService.getMeetSplitInfo(
                filterMeet, MeetSplitsOption.SecondToThirdMile, startDate, endDate, "lowest", limit))
    }

    @Async
    fun getProgressionFromMeetForAllRunnersBetweenDates(
            meetName: String,
            startDate: Date,
            endDate: Date,
            filterBy: String,
            adjustForDistance: Boolean
    ): Future<List<MeetProgressionDTO>> {

      return AsyncResult(meetProgressionService.getProgressionFromMeetForAllRunnersBetweenDates(meetName, startDate, endDate, filterBy, adjustForDistance))
    }

    @Async
    fun getImprovementRatesAtGivenMeet(seasonStartDate: Date, seasonEndDate: Date, givenMeet: String): Future<List<ImprovementRateDTO>> {
        return AsyncResult(improvementRateService.getImprovementRatesAtGivenMeet(seasonStartDate, seasonEndDate, givenMeet))
    }

}