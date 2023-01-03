package com.terkula.uaxctf.statistics.response.achievement

import com.terkula.uaxctf.statistics.dto.streak.StreakDTO

class RunnerAchievementsDTO(
    var prAchievements: List<Achievement>,
    var passesLastMileAchievements: List<Achievement>,
    var wonRaceAchievements: List<Achievement>,
    var consistentRaceAchievements: List<Achievement>,
    var totalTrainingDistanceAchievements: List<Achievement>,
    var loggedRunAchievement: List<Achievement>,
    var skullsEarnedStreak: StreakDTO,
    var totalSkullsEarnedAchievement: List<Achievement>
)




class Achievement(
    val threshold: Double,
    val value: Double,
    val met: Boolean
)
