package com.terkula.uaxctf.statistics.response

import com.terkula.uaxctf.statistics.dto.FasterAndSlowerProgressions
import com.terkula.uaxctf.statistics.dto.SummaryImprovementRateDTO

class MeetSummaryResponse(val seasonBests: SeasonBestResponse, val prs: PRResponse,
                          val newlyMetGoals: MetGoalResponse,
                          val summaryImprovementFromLastMeet: SummaryImprovementRateDTO,
                          val comparisonFromLastYear: FasterAndSlowerProgressions) {

}