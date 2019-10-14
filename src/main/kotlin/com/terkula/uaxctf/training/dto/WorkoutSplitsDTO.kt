package com.terkula.uaxctf.training.dto

import com.terkula.uaxctf.training.model.NumberedSplit

class WorkoutSplitsDTO(val splits: List<NumberedSplit>, val targetPace: String, val source: String,
                       val spread: String, val avgDifferenceFromTarget: String)