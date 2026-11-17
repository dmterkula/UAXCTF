package com.terkula.uaxctf.weather

import org.springframework.stereotype.Service
import kotlin.math.pow

/**
 * Ports the metabolic-cost-of-running-on-grade polynomial from Minetti et al. 2002,
 * "Energy cost of walking and running at extreme uphill and downhill slopes" (J Appl
 * Physiol 93: 1039-1046) -- a closed-form fit, valid roughly -45% to +45% grade, so
 * unlike [HeatPaceAdjustmentService] no lookup grid/resource file is needed.
 */
@Service
class CourseGradeAdjustmentService {

    /** Metabolic cost multiplier (1.0 = flat-ground cost) for running at the given grade (decimal, e.g. 0.08 for 8%). */
    fun costMultiplier(gradeDecimal: Double): Double {
        val i = gradeDecimal
        val cr = 155.4 * i.pow(5) - 30.4 * i.pow(4) - 43.3 * i.pow(3) + 46.3 * i.pow(2) + 19.5 * i + 3.6
        return cr / 3.6
    }

    /**
     * Forward/"effort" adjustment: given a flat-course goal time, what time does that same effort
     * actually produce on a course with this cost factor? Mirrors [HeatPaceAdjustmentService.adjustPlanTimeForHeat].
     */
    fun adjustPlanTimeForGrade(baseSeconds: Double, courseCostFactor: Double): Double =
        baseSeconds * courseCostFactor

    /**
     * Reverse/"pace" adjustment: given an actual time run on a course with this cost factor, what
     * would the equivalent flat-course time have been? Used to normalize race results for fair
     * cross-course comparison. Mirrors [HeatPaceAdjustmentService.normalizeResultTimeForHeat].
     */
    fun normalizeResultTimeForGrade(actualSeconds: Double, courseCostFactor: Double): Double =
        actualSeconds / courseCostFactor
}
