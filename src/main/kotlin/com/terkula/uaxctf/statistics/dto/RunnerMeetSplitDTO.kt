package com.terkula.uaxctf.statistics.dto

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
class RunnerMeetSplitDTO (val meetPerformanceDTO: MeetPerformanceDTO, val meetSplitsDTO: MeetSplitsDTO?)