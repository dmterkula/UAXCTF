package com.terkula.uaxctf.statistics.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.terkula.uaxctf.statisitcs.model.Runner

class MeetProgressionDTO (val runner: Runner,
                          @JsonProperty("performances") val meetPerformanceDTOs: List<MeetPerformanceDTO>,
                          val progression: List<String>)