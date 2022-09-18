package com.terkula.uaxctf.statistics.request


class GoalsRequest(val goals: List<GoalCreationElement>)

class UpdateGoalRequest(val existingGoal: GoalCreationElement, val updatedGoal: GoalCreationElement)

class GoalCreationElement(val type: String, val value: String, var isMet: Boolean = false)