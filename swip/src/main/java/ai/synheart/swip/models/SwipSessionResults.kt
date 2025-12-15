package ai.synheart.swip.models

import java.time.Instant

/**
 * SWIP Session Results
 */
data class SwipSessionResults(
    val sessionId: String,
    val scores: List<SwipScoreResult>,
    val emotions: List<EmotionResult>,
    val startTime: Instant,
    val endTime: Instant
) {
    /**
     * Get summary statistics
     */
    fun getSummary(): Map<String, Any> {
        if (scores.isEmpty()) {
            return mapOf(
                "duration_seconds" to 0,
                "average_swip_score" to 0.0,
                "dominant_emotion" to "Unknown"
            )
        }

        val avgScore = scores.map { it.swipScore }.average()
        val dominantEmotion = getMostFrequentEmotion()

        return mapOf(
            "session_id" to sessionId,
            "duration_seconds" to java.time.Duration.between(startTime, endTime).seconds,
            "average_swip_score" to avgScore,
            "dominant_emotion" to dominantEmotion,
            "score_count" to scores.size,
            "emotion_count" to emotions.size
        )
    }

    /**
     * Get most frequent emotion
     */
    private fun getMostFrequentEmotion(): String {
        if (scores.isEmpty()) return "Unknown"

        val emotionCounts = mutableMapOf<String, Int>()
        for (score in scores) {
            emotionCounts[score.dominantEmotion] = (emotionCounts[score.dominantEmotion] ?: 0) + 1
        }

        return emotionCounts.maxByOrNull { it.value }?.key ?: "Unknown"
    }
}

/**
 * SWIP Score Result
 */
data class SwipScoreResult(
    val swipScore: Double,
    val dominantEmotion: String,
    val emotionProbabilities: Map<String, Double>,
    val hrv: Double,
    val heartRate: Double,
    val timestamp: Instant,
    val confidence: Double = 0.0,
    val dataQuality: Double = 1.0
)

/**
 * Emotion Recognition Result
 */
data class EmotionResult(
    val emotion: String,
    val confidence: Double,
    val probabilities: Map<String, Double>,
    val timestamp: Instant
)
