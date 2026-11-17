package com.terkula.uaxctf.statistics.dto

import com.terkula.uaxctf.statisitcs.model.VenueCourseProfile
import java.text.SimpleDateFormat
import java.util.TimeZone

data class VenueCourseProfileResponse(
    val id: Int,
    val venueId: Int,
    val label: String?,
    val isCurrent: Boolean,
    val distanceMeters: Double?,
    val elevationGainFt: Double?,
    val elevationLossFt: Double?,
    val minElevationFt: Double?,
    val maxElevationFt: Double?,
    val maxGradePct: Double?,
    val minGradePct: Double?,
    val courseCostFactor: Double?,
    val createdAt: String?
) {
    companion object {
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").apply {
            timeZone = TimeZone.getTimeZone("America/New_York")
        }

        fun from(profile: VenueCourseProfile) = VenueCourseProfileResponse(
            id = profile.id,
            venueId = profile.venueId,
            label = profile.label,
            isCurrent = profile.isCurrent,
            distanceMeters = profile.distanceMeters,
            elevationGainFt = profile.elevationGainFt,
            elevationLossFt = profile.elevationLossFt,
            minElevationFt = profile.minElevationFt,
            maxElevationFt = profile.maxElevationFt,
            maxGradePct = profile.maxGradePct,
            minGradePct = profile.minGradePct,
            courseCostFactor = profile.courseCostFactor,
            createdAt = profile.createdAt?.let { dateFormat.format(it) }
        )
    }
}
