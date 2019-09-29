package com.terkula.uaxctf.statistics.dto

class SummaryImprovementRateDTO (val averageDifference: Double, val medianDifference: Double,
                                 val faster: ImprovementRatePair, val slower: ImprovementRatePair)