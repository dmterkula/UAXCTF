package com.terkula.uaxctf.training.request

import java.sql.Date
import javax.validation.Valid
import javax.validation.constraints.Pattern

class CreateWorkoutRequest (
    var date: Date,
    var title: String,
    var description: String,
    var icon: String,
    var uuid: String,
    @Valid
    var components: List<ComponentCreationElement>
    )

class ComponentCreationElement(
    var description: String,
    @Pattern(
            regexp = "Interval|Tempo|Progression|descriptionOnly",
            message = "The value provided for type is invalid. Valid values are 'Interval', 'Tempo' or 'Progression'")
    var type: String,
    @Pattern(
            regexp = "Goal|PR|SB|Season Avg",
            message = "The value provided for pace is invalid. Valid values are 'Goal', 'PR' or 'SB', or 'Season Avg'")
    var pace: String,
    var targetDistance: Int,
    var targetCount: Int,
    var duration: String?,
    var targetPaceAdjustment: String = "",
    var uuid: String
)