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
        val skullsStreak = skullsEarnedStreak.get()

        return RunnerAchievementsDTO(
                prAchievements = buildPrAchievementList(prs.get()),
                passesLastMileAchievements = buildPassesLastMileAchievements(totalPassesLastMile.get()),
                consistentRaceAchievements = buildConsistentRacesAchievements(consistentRaces.get().mileSplits.size),
                wonRaceAchievements = buildWonRacesAchievements(racesWon.get().size),
                totalTrainingDistanceAchievements = buildTrainingDistanceAchievements(tm.first),
                loggedRunAchievement = buildLoggedRunAchievements(tm.second),
                skullsEarnedStreak = skullsStreak,
                skullStreakAchievement = buildSkullStreakAchievements(skullsStreak.longestStreak),
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

    fun buildImageName(prefix: String, threshold: Int): String {
        return prefix + "-" + threshold.toString() + "x"
    }

    fun buildPrAchievementList(prs: List<MeetPerformanceDTO>): List<Achievement> {

        val achievements = mutableListOf<Achievement>()
        val imagePrefix = "pr"

        val description = "Total PRs Set"

        if (prs.size >= 5) {
            achievements.add(Achievement(5.0, prs.size.toDouble(), true, buildImageName(imagePrefix, 5), true, description, "all"))
        } else {
            achievements.add(Achievement(5.0, prs.size.toDouble(), false, buildImageName(imagePrefix, 5), true, description, "all"))
        }

        if (prs.size >= 10) {
            achievements.add(Achievement(10.0, prs.size.toDouble(), true, buildImageName(imagePrefix, 10), true, description, "all"))
        } else {
            achievements.add(Achievement(10.0, prs.size.toDouble(), false, buildImageName(imagePrefix, 5), true, description, "all"))
        }

        return achievements
    }

    fun buildPassesLastMileAchievements(passes: Int): List<Achievement> {

        val achievements = mutableListOf<Achievement>()
        val imagePrefix = "passes-last-mile"

        val description = "Total Passes 3rd Mi."

        if (passes >= 10) {
            achievements.add(Achievement(10.0, passes.toDouble(), true, buildImageName(imagePrefix, 10), true, description, "xc"))
        } else {
            achievements.add(Achievement(10.0, passes.toDouble(), false, buildImageName(imagePrefix, 10), true, description, "xc"))
        }

        if (passes >= 25) {
            achievements.add(Achievement(25.0, passes.toDouble(), true, buildImageName(imagePrefix, 25), true, description, "xc"))
        } else {
            achievements.add(Achievement(25.0, passes.toDouble(), false, buildImageName(imagePrefix, 25), true, description, "xc"))
        }

        if (passes >= 50) {
            achievements.add(Achievement(50.0, passes.toDouble(), true, buildImageName(imagePrefix, 50), true, description, "xc"))
        } else {
            achievements.add(Achievement(50.0, passes.toDouble(), false, buildImageName(imagePrefix, 50), true, description, "xc"))
        }

        if (passes >= 100) {
            achievements.add(Achievement(100.0, passes.toDouble(), true, buildImageName(imagePrefix, 100), true, description, "xc"))
        } else {
            achievements.add(Achievement(100.0, passes.toDouble(), false, buildImageName(imagePrefix, 100), true, description, "xc"))
        }

        return achievements
    }

    fun buildWonRacesAchievements(wonRaces: Int): List<Achievement> {

        val achievements = mutableListOf<Achievement>()
        val imagePrefix = "won-race"
        val description = "Total number of races won"

        if (wonRaces >= 1) {
            achievements.add(Achievement(1.0, wonRaces.toDouble(), true, buildImageName(imagePrefix, 1), true, description, "all"))
        } else {
            achievements.add(Achievement(1.0, wonRaces.toDouble(), false, buildImageName(imagePrefix, 1), true, description, "all"))
        }

        return achievements

    }


    fun buildConsistentRacesAchievements(races: Int): List<Achievement> {

        val achievements = mutableListOf<Achievement>()

        val imagePrefix = "consistent-race"
        val description = "Races where mile splits are within 20s of each other"

        if (races >= 1) {
            achievements.add(Achievement(1.0, races.toDouble(), true, buildImageName(imagePrefix, 1), true, description, "xc"))
        } else {
            achievements.add(Achievement(1.0, races.toDouble(), false, buildImageName(imagePrefix, 1), true, description, "xc"))
        }

        if (races >= 5) {
            achievements.add(Achievement(5.0, races.toDouble(), true, buildImageName(imagePrefix, 5),true, description, "xc"))
        } else {
            achievements.add(Achievement(5.0, races.toDouble(), false, buildImageName(imagePrefix, 5),true, description, "xc"))
        }

        if (races >= 10) {
            achievements.add(Achievement(10.0, races.toDouble(), true, buildImageName(imagePrefix, 10), true, description, "xc"))
        } else {
            achievements.add(Achievement(10.0, races.toDouble(), false, buildImageName(imagePrefix, 10), true, description, "xc"))
        }

        return achievements

    }

    fun buildTrainingDistanceAchievements(distance: Double): List<Achievement> {

        val imagePrefix = "training-distance"
        val description = "Total miles logged"

        val achievements = mutableListOf<Achievement>()

        if (distance >= 50) {
            achievements.add(Achievement(50.0, distance, true, buildImageName(imagePrefix, 50), false, description, "all"))
        } else {
            achievements.add(Achievement(50.0, distance, false, buildImageName(imagePrefix, 50), false, description, "all"))
        }

        if (distance >= 100) {
            achievements.add(Achievement(100.0, distance, true, buildImageName(imagePrefix, 100), false, description, "all"))
        } else {
            achievements.add(Achievement(100.0, distance, false, buildImageName(imagePrefix, 100), false, description, "all"))
        }

        if (distance >= 250) {
            achievements.add(Achievement(250.0, distance, true, buildImageName(imagePrefix, 250), false, description, "all"))
        } else {
            achievements.add(Achievement(250.0, distance, false, buildImageName(imagePrefix, 250), false, description, "all"))
        }

        if (distance >= 500) {
            achievements.add(Achievement(500.0, distance, true, buildImageName(imagePrefix, 500), false, description, "all"))
        } else {
            achievements.add(Achievement(500.0, distance, false, buildImageName(imagePrefix, 500), false, description, "all"))
        }

        return achievements

    }

    fun buildLoggedRunAchievements(runs: Int): List<Achievement> {

        val achievements = mutableListOf<Achievement>()
        val imagePrefix = "logged-run"
        val description = "Total runs logged"

        if (runs >= 25) {
            achievements.add(Achievement(25.0, runs.toDouble(), true, buildImageName(imagePrefix, 25), true, description, "all"))
        } else {
            achievements.add(Achievement(25.0, runs.toDouble(), false, buildImageName(imagePrefix, 25), true, description, "all"))
        }

        if (runs >= 50) {
            achievements.add(Achievement(50.0, runs.toDouble(), true, buildImageName(imagePrefix, 50), true, description, "all"))
        } else {
            achievements.add(Achievement(50.0, runs.toDouble(), false, buildImageName(imagePrefix, 50), true, description, "all"))
        }

        if (runs >= 75) {
            achievements.add(Achievement(75.0, runs.toDouble(), true, buildImageName(imagePrefix, 75), true, description, "all"))
        } else {
            achievements.add(Achievement(75.0, runs.toDouble(), false, buildImageName(imagePrefix, 75), true, description, "all"))
        }

        if (runs >= 100) {
            achievements.add(Achievement(100.0, runs.toDouble(), true, buildImageName(imagePrefix, 100), true, description, "all"))
        } else {
            achievements.add(Achievement(100.0, runs.toDouble(), false, buildImageName(imagePrefix, 100), true, description, "all"))
        }

        return achievements

    }

    fun buildSkullStreakAchievements(streak: Int): List<Achievement> {

        val achievements = mutableListOf<Achievement>()

        val imagePrefix = "skull-streak"

        val description = "Consecutive races with a skull earned"

        if (streak >= 3) {
            achievements.add(Achievement(3.0, streak.toDouble(), true, buildImageName(imagePrefix, 3), true, description, "xc"))
        } else {
            achievements.add(Achievement(3.0, streak.toDouble(), false, buildImageName(imagePrefix, 3), true, description, "xc"))
        }

        if (streak >= 7) {
            achievements.add(Achievement(7.0, streak.toDouble(), true, buildImageName(imagePrefix, 7), true, description, "xc"))
        } else {
            achievements.add(Achievement(7.0, streak.toDouble(), false, buildImageName(imagePrefix, 7), true, description, "xc"))
        }

        if (streak >= 10) {
            achievements.add(Achievement(10.0, streak.toDouble(), true, buildImageName(imagePrefix, 10), true, description, "xc"))
        } else {
            achievements.add(Achievement(10.0, streak.toDouble(), false, buildImageName(imagePrefix, 10), true, description, "xc"))
        }

        return achievements

    }

    fun buildSkullsEarnedAchievements(skulls: Int): List<Achievement> {

        val achievements = mutableListOf<Achievement>()

        val imagePrefix = "skulls-earned"

        val description = "Total skull beads earned"

        if (skulls >= 1) {
            achievements.add(Achievement(1.0, skulls.toDouble(), true, buildImageName(imagePrefix, 1), true, description, "xc"))
        } else {
            achievements.add(Achievement(1.0, skulls.toDouble(), false, buildImageName(imagePrefix, 1), true, description, "xc"))
        }

        if (skulls >= 5) {
            achievements.add(Achievement(5.0, skulls.toDouble(), true, buildImageName(imagePrefix, 5), true, description, "xc"))
        } else {
            achievements.add(Achievement(5.0, skulls.toDouble(), false, buildImageName(imagePrefix, 5), true, description, "xc"))
        }

        if (skulls >= 10) {
            achievements.add(Achievement(10.0, skulls.toDouble(), true, buildImageName(imagePrefix, 10), true, description, "xc"))
        } else {
            achievements.add(Achievement(10.0, skulls.toDouble(), false, buildImageName(imagePrefix, 10), true, description, "xc"))
        }

        if (skulls >= 25) {
            achievements.add(Achievement(25.0, skulls.toDouble(), true, buildImageName(imagePrefix, 25), true, description, "xc"))
        } else {
            achievements.add(Achievement(25.0, skulls.toDouble(), false, buildImageName(imagePrefix, 25), true, description, "xc"))
        }

        if (skulls >= 50) {
            achievements.add(Achievement(50.0, skulls.toDouble(), true, buildImageName(imagePrefix, 50), true, description, "xc"))
        } else {
            achievements.add(Achievement(50.0, skulls.toDouble(), false, buildImageName(imagePrefix, 50), true, description, "xc"))
        }

        return achievements

    }

}