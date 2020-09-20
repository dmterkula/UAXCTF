package com.terkula.uaxctf.statistics.response

import com.fasterxml.jackson.annotation.JsonProperty
import com.terkula.uaxctf.statistics.dto.RunnersMetGoals
import com.terkula.uaxctf.statistics.dto.UnMetGoalDTO

class MetGoalResponse(@JsonProperty("metGoals") val metGoalDTOs: List<RunnersMetGoals>)

class UnMetGoalResponse(@JsonProperty("UnMetGoals")val unMetGoalDTOs: List<UnMetGoalDTO>)