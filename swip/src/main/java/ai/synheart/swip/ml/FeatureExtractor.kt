package ai.synheart.swip.ml

import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Extracts HRV features from physiological data
 */
class FeatureExtractor {
    /**
     * Extract features from data window
     */
    fun extract(window: List<DataPoint>): DoubleArray {
        if (window.isEmpty()) {
            return DoubleArray(6) { 0.0 }
        }

        val hrs = window.map { it.hr }
        val hrvs = window.map { it.hrv }

        // Basic HR statistics
        val meanHR = hrs.average()
        val stdHR = calculateStdDev(hrs)
        val minHR = hrs.minOrNull() ?: 0.0
        val maxHR = hrs.maxOrNull() ?: 0.0

        // HRV metrics
        val sdnn = hrvs.average() // Simplified SDNN (using HRV values)
        val rmssd = calculateRMSSD(hrvs)

        return doubleArrayOf(
            meanHR,
            stdHR,
            minHR,
            maxHR,
            sdnn,
            rmssd
        )
    }

    private fun calculateStdDev(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0

        val mean = values.average()
        val variance = values.map { (it - mean).pow(2) }.average()
        return sqrt(variance)
    }

    private fun calculateRMSSD(values: List<Double>): Double {
        if (values.size < 2) return 0.0

        val squaredDiffs = mutableListOf<Double>()
        for (i in 0 until values.size - 1) {
            val diff = values[i + 1] - values[i]
            squaredDiffs.add(diff.pow(2))
        }

        return sqrt(squaredDiffs.average())
    }
}
