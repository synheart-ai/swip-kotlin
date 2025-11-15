package ai.synheart.swip.ml

import ai.synheart.swip.models.EmotionConfig
import ai.synheart.swip.models.EmotionResult
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Emotion Recognition Engine
 *
 * Processes HR/HRV data and predicts emotional states using on-device ML
 */
class EmotionEngine(
    private val config: EmotionConfig,
    private val context: android.content.Context? = null
) {
    private val featureExtractor = FeatureExtractor()
    private val svmPredictor = SvmPredictor(context)

    private val dataBuffer = ConcurrentLinkedQueue<DataPoint>()
    private val results = ConcurrentLinkedQueue<EmotionResult>()

    /**
     * Push new physiological data
     */
    fun push(
        hr: Double,
        hrv: Double,
        timestamp: Instant,
        motion: Double
    ) {
        dataBuffer.add(
            DataPoint(
                hr = hr,
                hrv = hrv,
                timestamp = timestamp,
                motion = motion
            )
        )

        // Process if we have enough data
        if (dataBuffer.size >= MIN_BUFFER_SIZE) {
            processBuffer()
        }
    }

    /**
     * Consume ready emotion results
     */
    fun consumeReady(): List<EmotionResult> {
        val ready = mutableListOf<EmotionResult>()
        while (results.isNotEmpty()) {
            results.poll()?.let { ready.add(it) }
        }
        return ready
    }

    /**
     * Clear all data
     */
    fun clear() {
        dataBuffer.clear()
        results.clear()
    }

    private fun processBuffer() {
        // Get latest window of data
        val window = dataBuffer.toList().takeLast(WINDOW_SIZE)

        // Extract features
        val features = featureExtractor.extract(window)

        // Predict emotion
        val prediction = svmPredictor.predict(features)

        // Create result
        val result = EmotionResult(
            emotion = prediction.emotion,
            confidence = prediction.confidence,
            probabilities = prediction.probabilities,
            timestamp = Instant.now()
        )

        // Only add if confidence exceeds threshold
        if (result.confidence >= config.confidenceThreshold) {
            results.add(result)
        }

        // Remove old data to prevent memory growth
        while (dataBuffer.size > MAX_BUFFER_SIZE) {
            dataBuffer.poll()
        }
    }

    companion object {
        private const val MIN_BUFFER_SIZE = 10
        private const val WINDOW_SIZE = 60
        private const val MAX_BUFFER_SIZE = 300
    }
}

/**
 * Data point for emotion recognition
 */
data class DataPoint(
    val hr: Double,
    val hrv: Double,
    val timestamp: Instant,
    val motion: Double
)

/**
 * Emotion prediction result
 */
data class EmotionPrediction(
    val emotion: String,
    val confidence: Double,
    val probabilities: Map<String, Double>
)
