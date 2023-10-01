package com.terkula.uaxctf.statistics.response.track

import com.fasterxml.jackson.annotation.JsonProperty
import com.terkula.uaxctf.statistics.dto.track.TrackPRsDTO

class TrackPRResponse (val count: Int, @JsonProperty("PRs") val prs: List<TrackPRsDTO?>)