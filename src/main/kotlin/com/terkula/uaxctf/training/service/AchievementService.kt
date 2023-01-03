package com.terkula.uaxctf.training.service

import com.terkula.uaxctf.statistics.dto.MeetPerformanceDTO
import com.terkula.uaxctf.statistics.response.achievement.Achievement
import com.terkula.uaxctf.statistics.response.achievement.RunnerAchievementsDTO
import org.springframework.stereotype.Service

@Service
class AchievementService(
    val asyncAchievementService: AsyncAchievementService
) {

    fun getRunnersAchievements(runnerId: Int): RunnerAchievementsDTO {

        //// async ops
        val prs = asyncAchievementService.getTotalPrsSet(runnerId)
        val totalPassesLastMile = asyncAchievementService.getTotalPassesLastMile(runnerId)
        val consistentRaces = asyncAchievementService.getConsistentRaces(runnerId, 20)
        val totalMiles = asyncAchievementService.getRunnersTotalTrainingDistance(runnerId)
        val racesWon = asyncAchievementService.wonRaces(runnerId)
        var skullsEarnedStreak = asyncAchievementService.getSkullsEarnedStreak(runnerId)
        var totalSkulls = asyncAchievementService.getSkullsEarnedTotal(runnerId)

        /////

        val tm = totalMiles.get()

        return RunnerAchievementsDTO(
                prAchievements = buildPrAchievementList(prs.get()),
                passesLastMileAchievements = buildPassesLastMileAchievements(totalPassesLastMile.get()),
                consistentRaceAchievements = buildConsistentRacesAchievements(consistentRaces.get().mileSplits.size),
                wonRaceAchievements = buildWonRacesAchievements(racesWon.get().size),
                totalTrainingDistanceAchievements = buildTrainingDistanceAchievements(tm.first),
                loggedRunAchievement = buildLoggedRunAchievements(tm.second),
                skullsEarnedStreak = skullsEarnedStreak.get(),
                totalSkullsEarnedAchievement = buildSkullsEarnedAchievements(totalSkulls.get())
        )
    }

    //
//    var prFive: Achievement,
//    var prTen: Achievement,
//
//    var passesLastMile10: Achievement,
//    var passesLastMile25: Achievement,
//    var passesLastMile50: Achievement,
//    var passesLastMile100: Achievement,
//
//    var wonRace: Achievement,
//    var consistentRaces1: Achievement,
//    var consistentRaces5: Achievement,
//    var consistentRaces10: Achievement,
//
//    var totalTrainingDistance25: Achievement,
//    var totalTrainingDistance50: Achievement,
//    var totalTrainingDistance100: Achievement,
//    var totalTrainingDistance250: Achievement,
//    var totalTrainingDistance500: Achievement,
//
//    var loggedRun25: Achievement,
//    var loggedRun50: Achievement,
//    var loggedRun100: Achievement,
//
//    var skullStreak3: Achievement,
//    var skullStreak7: Achievement,
//    var skullStreak10: Achievement,

    fun buildPrAchievementList(prs: List<MeetPerformanceDTO>): List<Achievement> {

        val achievements = mutableListOf<Achievement>()

        if (prs.size >= 5) {
            achievements.add(Achievement(5.0, prs.size.toDouble(), true))
        } else {
            achievements.add(Achievement(5.0, prs.size.toDouble(), false))
        }

        if (prs.size >= 10) {
            achievements.add(Achievement(10.0, prs.size.toDouble(), true))
        } else {
            achievements.add(Achievement(10.0, prs.size.toDouble(), false))
        }

        return achievements
    }

    fun buildPassesLastMileAchievements(passes: Int): List<Achievement> {

        val achievements = mutableListOf<Achievement>()

        if (passes >= 10) {
            achievements.add(Achievement(10.0, passes.toDouble(), true))
        } else {
            achievements.add(Achievement(10.0, passes.toDouble(), false))
        }

        if (passes >= 25) {
            achievements.add(Achievement(25.0, passes.toDouble(), true))
        } else {
            achievements.add(Achievement(25.0, passes.toDouble(), false))
        }

        if (passes >= 50) {
            achievements.add(Achievement(50.0, passes.toDouble(), true))
        } else {
            achievements.add(Achievement(50.0, passes.toDouble(), false))
        }

        if (passes >= 100) {
            achievements.add(Achievement(100.0, passes.toDouble(), true))
        } else {
            achievements.add(Achievement(100.0, passes.toDouble(), false))
        }

        return achievements
    }

    fun buildWonRacesAchievements(wonRaces: Int): List<Achievement> {

        val achievements = mutableListOf<Achievement>()

        if (wonRaces >= 1) {
            achievements.add(Achievement(1.0, wonRaces.toDouble(), true))
        } else {
            achievements.add(Achievement(1.0, wonRaces.toDouble(), false))
        }

        return achievements

    }


    fun buildConsistentRacesAchievements(races: Int): List<Achievement> {

        val achievements = mutableListOf<Achievement>()

        if (races >= 1) {
            achievements.add(Achievement(1.0, races.toDouble(), true))
        } else {
            achievements.add(Achievement(1.0, races.toDouble(), false))
        }

        if (races >= 5) {
            achievements.add(Achievement(5.0, races.toDouble(), true))
        } else {
            achievements.add(Achievement(5.0, races.toDouble(), false))
        }

        if (races >= 10) {
            achievements.add(Achievement(10.0, races.toDouble(), true))
        } else {
            achievements.add(Achievement(10.0, races.toDouble(), false))
        }

        return achievements

    }

    fun buildTrainingDistanceAchievements(distance: Double): List<Achievement> {

        val achievements = mutableListOf<Achievement>()

        if (distance >= 50) {
            achievements.add(Achievement(50.0, distance, true))
        } else {
            achievements.add(Achievement(50.0, distance, false))
        }

        if (distance >= 100) {
            achievements.add(Achievement(100.0, distance, true))
        } else {
            achievements.add(Achievement(100.0, distance, false))
        }

        if (distance >= 250) {
            achievements.add(Achievement(250.0, distance, true))
        } else {
            achievements.add(Achievement(250.0, distance, false))
        }

        if (distance >= 500) {
            achievements.add(Achievement(500.0, distance, true))
        } else {
            achievements.add(Achievement(500.0, distance, false))
        }

        return achievements

    }

    fun buildLoggedRunAchievements(runs: Int): List<Achievement> {

        val achievements = mutableListOf<Achievement>()

        if (runs >= 25) {
            achievements.add(Achievement(25.0, runs.toDouble(), true))
        } else {
            achievements.add(Achievement(25.0, runs.toDouble(), false))
        }

        if (runs >= 50) {
            achievements.add(Achievement(50.0, runs.toDouble(), true))
        } else {
            achievements.add(Achievement(50.0, runs.toDouble(), false))
        }

        if (runs >= 100) {
            achievements.add(Achievement(100.0, runs.toDouble(), true))
        } else {
            achievements.add(Achievement(100.0, runs.toDouble(), false))
        }

        return achievements

    }

    fun buildSkullStreakAchievements(streak: Int): List<Achievement> {

        val achievements = mutableListOf<Achievement>()

        if (streak >= 3) {
            achievements.add(Achievement(3.0, streak.toDouble(), true))
        } else {
            achievements.add(Achievement(3.0, streak.toDouble(), false))
        }

        if (streak >= 7) {
            achievements.add(Achievement(7.0, streak.toDouble(), true))
        } else {
            achievements.add(Achievement(7.0, streak.toDouble(), false))
        }

        if (streak >= 10) {
            achievements.add(Achievement(10.0, streak.toDouble(), true))
        } else {
            achievements.add(Achievement(10.0, streak.toDouble(), false))
        }

        return achievements

    }

    fun buildSkullsEarnedAchievements(skulls: Int): List<Achievement> {

        val achievements = mutableListOf<Achievement>()

        if (skulls >= 5) {
            achievements.add(Achievement(5.0, skulls.toDouble(), true))
        } else {
            achievements.add(Achievement(5.0, skulls.toDouble(), false))
        }

        if (skulls >= 10) {
            achievements.add(Achievement(10.0, skulls.toDouble(), true))
        } else {
            achievements.add(Achievement(10.0, skulls.toDouble(), false))
        }

        if (skulls >= 25) {
            achievements.add(Achievement(25.0, skulls.toDouble(), true))
        } else {
            achievements.add(Achievement(25.0, skulls.toDouble(), false))
        }

        return achievements

    }






}