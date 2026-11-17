package com.terkula.uaxctf.weather

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.terkula.uaxctf.util.calculateSecondsFrom
import com.terkula.uaxctf.util.toMinuteSecondString
import org.springframework.stereotype.Service
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

data class HeatPaceGrid(
    val air_temp_c: List<Double>,
    val humidity_pct: List<Double>,
    val logspeed_adjust: List<Double>
)

/**
 * Ports the heat/humidity -> pace slowdown model from apps.runningwritings.com's
 * free calculator (John J Davis, PhD / Running Writings), which is itself fit from
 * Mantzios et al. 2022 marathon heat/humidity data (open-access). The site does not
 * publish the underlying regression, only the sampled grid + bilinear interpolation
 * used to serve its calculator (extracted from its client-side scripts.js), which is
 * what's reproduced below.
 */
@Service
class HeatPaceAdjustmentService {

    companion object {
        /** At or under this effort distance, the penalty is scaled by [SHORT_EFFORT_SCALING]. */
        const val SHORT_EFFORT_DISTANCE_METERS = 5000

        /**
         * Scales down the log-speed penalty for efforts of [SHORT_EFFORT_DISTANCE_METERS] or less.
         *
         * The underlying grid is fit from marathon data (Mantzios et al. 2022), where runners are
         * exposed to the conditions for 2-5 hours and accumulate a large heat debt. A 5k is 20-35
         * minutes and an interval rep is far shorter, so much less heat accumulates and the
         * marathon-fit curve overstates the penalty at these distances. Halving keeps the model's
         * shape -- humidity still dominates air temperature -- while bringing the magnitude into a
         * defensible range. Longer efforts (e.g. a 7-mile tempo) sit closer to the fitted regime
         * and keep the full penalty.
         */
        const val SHORT_EFFORT_SCALING = 0.5

        /** XC races here are 5k or shorter, so they always take the short-effort scaling. */
        const val XC_RACE_DISTANCE_METERS = 5000
    }

    private val xVals: List<Double>
    private val yVals: List<Double>
    private val zByKey: Map<Pair<Double, Double>, Double>

    init {
        val mapper = ObjectMapper().registerKotlinModule()
        val stream = javaClass.classLoader.getResourceAsStream("heat_pace_grid.json")
            ?: throw IllegalStateException("heat_pace_grid.json missing from resources")
        val grid = mapper.readValue(stream, HeatPaceGrid::class.java)

        xVals = grid.air_temp_c.distinct().sorted()
        yVals = grid.humidity_pct.distinct().sorted()
        zByKey = grid.air_temp_c.indices.associate { i ->
            (grid.air_temp_c[i] to grid.humidity_pct[i]) to grid.logspeed_adjust[i]
        }
    }

    private fun bracket(arr: List<Double>, q: Double): Triple<Int, Int, Double> {
        val n = arr.size
        if (q <= arr[0]) {
            return Triple(0, 1, (q - arr[0]) / (arr[1] - arr[0]))
        }
        if (q >= arr[n - 1]) {
            return Triple(n - 2, n - 1, (q - arr[n - 2]) / (arr[n - 1] - arr[n - 2]))
        }
        var lo = 0
        var hi = n - 1
        while (hi - lo > 1) {
            val mid = (lo + hi) / 2
            if (arr[mid] >= q) hi = mid else lo = mid
        }
        val t = (q - arr[lo]) / (arr[hi] - arr[lo])
        return Triple(lo, hi, t)
    }

    /** Bilinear-interpolated (extrapolated at the edges) log-speed adjustment for the given conditions. */
    fun logSpeedAdjustment(tempC: Double, humidityPct: Double): Double {
        val clampedHumidity = max(0.0, min(100.0, humidityPct))
        val (ix0, ix1, tx) = bracket(xVals, tempC)
        val (iy0, iy1, ty) = bracket(yVals, clampedHumidity)

        val z11 = zByKey.getValue(xVals[ix0] to yVals[iy0])
        val z21 = zByKey.getValue(xVals[ix1] to yVals[iy0])
        val z12 = zByKey.getValue(xVals[ix0] to yVals[iy1])
        val z22 = zByKey.getValue(xVals[ix1] to yVals[iy1])

        val a = z11 * (1 - tx) + z21 * tx
        val b = z12 * (1 - tx) + z22 * tx
        return a * (1 - ty) + b * ty
    }

    /**
     * [logSpeedAdjustment], scaled down for short efforts. See [SHORT_EFFORT_SCALING] for why.
     */
    fun scaledLogSpeedAdjustment(tempC: Double, humidityPct: Double, targetDistanceMeters: Int): Double {
        val raw = logSpeedAdjustment(tempC, humidityPct)
        return if (targetDistanceMeters <= SHORT_EFFORT_DISTANCE_METERS) raw * SHORT_EFFORT_SCALING else raw
    }

    /**
     * Forward/"effort" adjustment: given a flat/ideal-condition goal pace, what pace does that
     * same effort actually produce in these conditions? Used for training run / workout plan paces.
     * Returns seconds for the same distance as [baseSeconds] (heat makes this LARGER). Scaled for
     * short efforts -- see [SHORT_EFFORT_SCALING].
     */
    fun adjustPlanTimeForHeat(baseSeconds: Double, tempC: Double, humidityPct: Double, targetDistanceMeters: Int): Double {
        val adjustment = scaledLogSpeedAdjustment(tempC, humidityPct, targetDistanceMeters)
        val logSpeed = ln(1.0 / baseSeconds) + adjustment
        val speed = exp(logSpeed)
        return 1.0 / speed
    }

    /**
     * Reverse/"pace" adjustment: given an actual time run in these conditions, what would the
     * equivalent flat/ideal-condition time have been? Used to normalize XC race results for
     * fair comparison across different weather. Returns seconds (heat makes this SMALLER, i.e.
     * the heat penalty is removed). Scaled for short efforts -- see [SHORT_EFFORT_SCALING].
     */
    fun normalizeResultTimeForHeat(actualSeconds: Double, tempC: Double, humidityPct: Double, targetDistanceMeters: Int = XC_RACE_DISTANCE_METERS): Double {
        val adjustment = scaledLogSpeedAdjustment(tempC, humidityPct, targetDistanceMeters)
        val logSpeed = ln(1.0 / actualSeconds) - adjustment
        val speed = exp(logSpeed)
        return 1.0 / speed
    }

    fun adjustPlanTimeForHeat(baseTime: String, tempC: Double, humidityPct: Double, targetDistanceMeters: Int): String =
        adjustPlanTimeForHeat(baseTime.calculateSecondsFrom(), tempC, humidityPct, targetDistanceMeters).toMinuteSecondString()

    fun normalizeResultTimeForHeat(actualTime: String, tempC: Double, humidityPct: Double, targetDistanceMeters: Int = XC_RACE_DISTANCE_METERS): String =
        normalizeResultTimeForHeat(actualTime.calculateSecondsFrom(), tempC, humidityPct, targetDistanceMeters).toMinuteSecondString()
}
