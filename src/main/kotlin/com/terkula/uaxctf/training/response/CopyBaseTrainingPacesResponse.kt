package com.terkula.uaxctf.training.response

import com.fasterxml.jackson.annotation.JsonInclude
import com.terkula.uaxctf.statisitcs.model.Runner

class CopyBaseTrainingPacesResponse(
    val dryRun: Boolean,
    val fromSeason: String,
    val fromYear: String,
    val fromEvent: String,
    val toSeason: String,
    val toYear: String,
    val toEvent: String,
    val adjustmentType: String,
    val adjustmentValue: Double,
    val createdCount: Int,
    val skippedCount: Int,
    val results: List<CopyBaseTrainingPaceResult>
)

/**
 * status:
 *  - WILL_CREATE / CREATED: no target pace yet
 *  - WILL_REPLACE / REPLACED: a target pace exists and overwriteExisting = true
 *  - SKIPPED_EXISTING: a target pace exists and overwriteExisting = false
 *  - NO_SOURCE: runner has no source pace to copy
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
class CopyBaseTrainingPaceResult(
    val runner: Runner,
    val status: String,
    val fractionOfMiles: Double?,
    val sourceSeconds: Int?,
    val sourcePacePerMile: String?,
    val newSeconds: Int?,
    val newPacePerMile: String?,
    val existingSeconds: Int?,
    val existingPacePerMile: String?,
    val existingSource: String?
)
