package com.terkula.uaxctf.statistics.dto

class SummaryImprovementRateDTO (val averageDifference: String, val medianDifference: String,
                                 val faster: ImprovementRatePair, val slower: ImprovementRatePair)