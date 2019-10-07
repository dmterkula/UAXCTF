package com.terkula.uaxctf.statistics.dto

import com.terkula.uaxctf.statisitcs.model.Runner

class RunnerAvgMileSplitDifferenceDTO(val runner: Runner, val whichSplitDifferenceTaken: String, val avgDifference: String,
                                      val numMeets: Int)