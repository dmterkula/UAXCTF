package com.terkula.uaxctf.statistics.response

import com.fasterxml.jackson.annotation.JsonProperty
import com.terkula.uaxctf.statistics.dto.PRDTO

class PRResponse (val count: Int, @JsonProperty("PRs") val prs: List<PRDTO>)