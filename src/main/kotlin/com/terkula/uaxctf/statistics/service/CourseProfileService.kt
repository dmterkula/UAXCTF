package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statisitcs.model.VenueCourseProfile
import com.terkula.uaxctf.statistics.repository.VenueCourseProfileRepository
import com.terkula.uaxctf.weather.CourseGradeAdjustmentService
import org.springframework.stereotype.Service
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private data class TrackPoint(val lat: Double, val lon: Double, val eleMeters: Double)

private const val EARTH_RADIUS_METERS = 6371000.0
private const val METERS_TO_FEET = 3.28084
// Below this spacing, watch GPS/barometric jitter at a near-stationary point (e.g. stopped at the
// finish) dominates the elevation delta and produces spurious grades of 100%+ -- real running/walking
// segments between consecutive ~1Hz watch pings are comfortably above this, so it only screens out noise.
private const val MIN_SEGMENT_DISTANCE_METERS = 1.0

@Service
class CourseProfileService(
    private val venueCourseProfileRepository: VenueCourseProfileRepository,
    private val courseGradeAdjustmentService: CourseGradeAdjustmentService
) {

    fun getProfilesForVenue(venueId: Int): List<VenueCourseProfile> =
        venueCourseProfileRepository.findByVenueId(venueId)

    fun getCurrentProfileForVenue(venueId: Int): VenueCourseProfile? =
        venueCourseProfileRepository.findByVenueIdAndIsCurrentTrue(venueId)

    fun createProfile(venueId: Int, label: String?, gpxContent: String, setAsCurrent: Boolean = true): VenueCourseProfile {
        val points = parseGpx(gpxContent)
        val stats = computeStats(points)

        if (setAsCurrent) {
            venueCourseProfileRepository.findByVenueId(venueId).forEach {
                it.isCurrent = false
                venueCourseProfileRepository.save(it)
            }
        }

        val profile = VenueCourseProfile(
            venueId = venueId,
            label = label,
            isCurrent = setAsCurrent,
            gpxContent = gpxContent,
            distanceMeters = stats.distanceMeters,
            elevationGainFt = stats.elevationGainFt,
            elevationLossFt = stats.elevationLossFt,
            minElevationFt = stats.minElevationFt,
            maxElevationFt = stats.maxElevationFt,
            maxGradePct = stats.maxGradePct,
            minGradePct = stats.minGradePct,
            courseCostFactor = stats.courseCostFactor,
            createdAt = java.sql.Timestamp(System.currentTimeMillis())
        )

        return venueCourseProfileRepository.save(profile)
    }

    fun setCurrent(id: Int): VenueCourseProfile? {
        val profile = venueCourseProfileRepository.findById(id).orElse(null) ?: return null
        venueCourseProfileRepository.findByVenueId(profile.venueId).forEach {
            it.isCurrent = it.id == profile.id
            venueCourseProfileRepository.save(it)
        }
        return venueCourseProfileRepository.findById(id).orElse(null)
    }

    fun deleteProfile(id: Int) = venueCourseProfileRepository.deleteById(id)

    private fun parseGpx(gpxContent: String): List<TrackPoint> {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val document = builder.parse(ByteArrayInputStream(gpxContent.toByteArray(StandardCharsets.UTF_8)))
        val trackPointNodes = document.getElementsByTagName("trkpt")

        return (0 until trackPointNodes.length).map { i ->
            val node = trackPointNodes.item(i) as Element
            val lat = node.getAttribute("lat").toDouble()
            val lon = node.getAttribute("lon").toDouble()
            val eleNodes = node.getElementsByTagName("ele")
            val ele = if (eleNodes.length > 0) eleNodes.item(0).textContent.trim().toDouble() else 0.0
            TrackPoint(lat, lon, ele)
        }
    }

    private fun haversineMeters(a: TrackPoint, b: TrackPoint): Double {
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLon = Math.toRadians(b.lon - a.lon)
        val lat1 = Math.toRadians(a.lat)
        val lat2 = Math.toRadians(b.lat)

        val h = sin(dLat / 2).pow2() + cos(lat1) * cos(lat2) * sin(dLon / 2).pow2()
        val c = 2 * atan2(sqrt(h), sqrt(1 - h))
        return EARTH_RADIUS_METERS * c
    }

    private fun Double.pow2() = this * this

    private class CourseStats(
        val distanceMeters: Double,
        val elevationGainFt: Double,
        val elevationLossFt: Double,
        val minElevationFt: Double,
        val maxElevationFt: Double,
        val maxGradePct: Double,
        val minGradePct: Double,
        val courseCostFactor: Double
    )

    private fun computeStats(points: List<TrackPoint>): CourseStats {
        require(points.size >= 2) { "GPX track must contain at least 2 points" }

        var totalDistanceMeters = 0.0
        var gainMeters = 0.0
        var lossMeters = 0.0
        var maxGradePct = Double.NEGATIVE_INFINITY
        var minGradePct = Double.POSITIVE_INFINITY
        var weightedCostSum = 0.0

        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]
            val segmentDistanceMeters = haversineMeters(prev, curr)
            if (segmentDistanceMeters < MIN_SEGMENT_DISTANCE_METERS) continue

            val elevationDelta = curr.eleMeters - prev.eleMeters
            if (elevationDelta > 0) gainMeters += elevationDelta else lossMeters += -elevationDelta

            val gradeDecimal = elevationDelta / segmentDistanceMeters
            val gradePct = gradeDecimal * 100.0
            maxGradePct = maxOf(maxGradePct, gradePct)
            minGradePct = minOf(minGradePct, gradePct)

            val cost = courseGradeAdjustmentService.costMultiplier(gradeDecimal)
            weightedCostSum += cost * segmentDistanceMeters
            totalDistanceMeters += segmentDistanceMeters
        }

        val elevations = points.map { it.eleMeters }
        val courseCostFactor = if (totalDistanceMeters > 0) weightedCostSum / totalDistanceMeters else 1.0

        return CourseStats(
            distanceMeters = totalDistanceMeters,
            elevationGainFt = gainMeters * METERS_TO_FEET,
            elevationLossFt = lossMeters * METERS_TO_FEET,
            minElevationFt = (elevations.minOrNull() ?: 0.0) * METERS_TO_FEET,
            maxElevationFt = (elevations.maxOrNull() ?: 0.0) * METERS_TO_FEET,
            maxGradePct = if (maxGradePct.isFinite()) maxGradePct else 0.0,
            minGradePct = if (minGradePct.isFinite()) minGradePct else 0.0,
            courseCostFactor = courseCostFactor
        )
    }
}
