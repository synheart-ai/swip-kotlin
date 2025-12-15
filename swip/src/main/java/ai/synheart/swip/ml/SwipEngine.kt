package ai.synheart.swip.ml

import ai.synheart.swip.models.SwipConfig
import ai.synheart.swip.models.SwipScoreResult
import java.time.Instant

/**
 * SWIP Score computation engine
 *
 * Computes wellness impact scores based on HRV, emotion, and physiological data
 */
class SwipEngine(private val config: SwipConfig) {
    /**
     * Compute SWIP score
     *
     * @param hr Heart rate (BPM)
     * @param hrv Heart rate variability (SDNN in ms)
     * @param motion Motion magnitude
     * @param emotionProbabilities Emotion prediction probabilities
     * @return SWIP score result
     */
    fun computeScore(
        hr: Double,
        hrv: Double,
        motion: Double,
        emotionProbabilities: Map<String, Double>
    ): SwipScoreResult {
        // Get dominant emotion
        val dominantEmotion = emotionProbabilities.maxByOrNull { it.value }?.key ?: "Unknown"
        val emotionConfidence = emotionProbabilities[dominantEmotion] ?: 0.0

        // Compute baseline HRV score (normalized to 0-100)
        val hrvScore = normalizeHRV(hrv)

        // Compute coherence score based on emotion
        val coherenceScore = computeCoherence(emotionProbabilities)

        // Weighted combination (normalize weights to sum to 1.0)
        val totalWeight = config.weightHrv + config.weightCoherence
        val normalizedWeightHrv = config.weightHrv / totalWeight
        val normalizedWeightCoherence = config.weightCoherence / totalWeight
        
        val swipScore = (
            normalizedWeightHrv * hrvScore +
            normalizedWeightCoherence * coherenceScore
        ).coerceIn(0.0, 100.0)

        // Compute data quality (simplified - based on HR validity)
        val dataQuality = if (hr in 40.0..200.0) 1.0 else 0.5

        return SwipScoreResult(
            swipScore = swipScore,
            dominantEmotion = dominantEmotion,
            emotionProbabilities = emotionProbabilities,
            hrv = hrv,
            heartRate = hr,
            timestamp = Instant.now(),
            confidence = emotionConfidence,
            dataQuality = dataQuality
        )
    }

    /**
     * Normalize HRV to 0-100 scale
     */
    private fun normalizeHRV(hrv: Double): Double {
        // Typical SDNN range is 20-100ms
        // Higher HRV = better (more relaxed)
        val normalized = ((hrv - 20.0) / 80.0) * 100.0
        return normalized.coerceIn(0.0, 100.0)
    }

    /**
     * Compute coherence score from emotion probabilities
     */
    private fun computeCoherence(emotionProbabilities: Map<String, Double>): Double {
        // Positive emotions (Amused, Calm) increase coherence
        // Negative emotions (Stressed) decrease coherence
        val positiveWeight = (emotionProbabilities["Amused"] ?: 0.0) * 1.0 +
                             (emotionProbabilities["Calm"] ?: 0.0) * 0.9
        val negativeWeight = (emotionProbabilities["Stressed"] ?: 0.0) * 0.2

        val coherence = (positiveWeight - negativeWeight) * 100.0
        return coherence.coerceIn(0.0, 100.0)
    }
}
