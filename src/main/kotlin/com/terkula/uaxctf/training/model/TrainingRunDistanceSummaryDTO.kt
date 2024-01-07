package com.terkula.uaxctf.training.model

import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.LocalDate

class TrainingRunDistanceSummaryDTO(
        var totalDistance: Double,
        var count: Int,
        var trainingCount: Int,
        var trainingRunDistance: Double,
        var avgSecondsPerMile: Double,
        @JsonIgnore var startDate: LocalDate
        )

class DateRangeRunSummaryDTO(
        var startDate: LocalDate,
        var endDate: LocalDate,
        var totalDistance: Double,
        var totalCount: Int,
        var trainingAvgPace: String,
        @JsonIgnore
        var totalSeconds: Double = 0.0
)