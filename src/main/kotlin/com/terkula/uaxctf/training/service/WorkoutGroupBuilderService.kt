package com.terkula.uaxctf.training.service

import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.statistics.service.SeasonBestService

import com.terkula.uaxctf.util.calculateSecondsFrom
import com.terkula.uaxctf.util.calculateSpreadWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.sql.Date
import java.util.stream.IntStream

@Service
class WorkoutGroupBuilderService (
        @field:Autowired
        val seasonBestService: SeasonBestService) {

    fun buildWorkoutGroups(season: String, maxVariance: Int, minGroupSize: Int, maxGroupSize: Int,
                           allowedFuzzinessFactor: Double) {

        val startDate = Date.valueOf("$season-01-01")
        val endDate = Date.valueOf("$season-12-31")

        val maximumAllowedVariance = maxVariance * allowedFuzzinessFactor

        val runnerToSeasonBestTime =
                seasonBestService
                        .getAllSeasonBests(startDate, endDate, false)
                        .map { it.runner to it.seasonBest.first().time.calculateSecondsFrom() }
                        .toMutableList()
                        .sortedBy { it.second }


       var groups: MutableList<MutableList<Pair<Runner, Double>>> = mutableListOf()

        // provided partitions to fill
        IntStream
                .range(0, maxGroupSize * 5)
                .forEach { groups.add(mutableListOf()) }

        var currentPartition = 0

        runnerToSeasonBestTime.forEach {
            // if current group is within max variance is less than max group size, add current runner to group
            if (groups[currentPartition].calculateSpreadWith(it) <= maxVariance && groups[currentPartition].size < maxGroupSize) {
                groups[currentPartition].add(it)
            } else {
                // increment the group number and add the current runner as the first runner in the new group.
                currentPartition ++
                groups[currentPartition].add(it)
            }
        }

        groups = groups.filter { it.isNotEmpty() }.toMutableList()

        val partitionsToBeMerged: MutableList<Pair<Int, Int>> = mutableListOf()

        IntStream.range(0, groups.size).forEach {
            if (groups[it].size == 1) {
                // candidate for merging
                if (it == 0 && groups.size > 1) {
                    // check only right partition for merging
                    if (groups[it + 1].calculateSpreadWith(groups[it].first()) <= maximumAllowedVariance) {
                        partitionsToBeMerged.add(it to 1)
                    }

                } else if(it == groups.size - 1) {
                    // check only left partition for merging
                    if (groups[it - 1].calculateSpreadWith(groups[it].first()) <= maximumAllowedVariance) {
                        partitionsToBeMerged.add((it - 1) to it)
                    }
                } else {

                    val leftVariance = groups[it - 1].calculateSpreadWith(groups[it].first())
                    val rightVariance = groups[it + 1].calculateSpreadWith(groups[it].first())

                    // if could swing to lower or higher group, swing to group causing least variance
                    if (leftVariance <= maximumAllowedVariance && rightVariance <= maximumAllowedVariance) {

                        if (leftVariance <= rightVariance) {
                            partitionsToBeMerged.add((it - 1) to it)
                        } else {
                            partitionsToBeMerged.add(it to (it + 1))
                        }

                        // swing to lower group
                    } else if (leftVariance <= maximumAllowedVariance) {
                        partitionsToBeMerged.add((it - 1) to it)
                        // swing to higher group
                    } else if (rightVariance <= maximumAllowedVariance) {
                        partitionsToBeMerged.add(it to (it + 1))
                    }
                }
            }

        }

        var partitionMergedCounter = 0
        partitionsToBeMerged.forEach {

            if (partitionMergedCounter == 0) {
                groups[it.first].addAll(groups[it.second])

            } else {
                groups[it.first - partitionMergedCounter].addAll(groups[it.second - partitionMergedCounter])
            }
            groups.remove(groups[it.second - partitionMergedCounter])
            partitionMergedCounter++
        }

        val test = groups.size
        var blah = test*2

    }

}
