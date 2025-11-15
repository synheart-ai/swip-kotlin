package ai.synheart.swip.ml

import android.content.Context
import kotlin.math.exp

/**
 * Linear SVM Predictor for emotion recognition
 *
 * Implements One-vs-Rest Linear SVM for 3 classes: Amused, Calm, Stressed
 */
class SvmPredictor(context: Context? = null) {
    // Model weights and biases loaded from assets
    private val weights: Map<String, DoubleArray>
    private val biases: Map<String, Double>
    private val featureMeans: DoubleArray
    private val featureStds: DoubleArray

    init {
        if (context != null) {
            // Load actual model from assets
            val loader = ModelLoader(context)
            val model = loader.loadSvmModel()

            // Convert model to internal format
            weights = model.classes.mapIndexed { index, className ->
                className to model.weights[index].toDoubleArray()
            }.toMap()

            biases = model.classes.mapIndexed { index, className ->
                className to model.bias[index]
            }.toMap()

            featureMeans = model.scalerMean.toDoubleArray()
            featureStds = model.scalerScale.toDoubleArray()
        } else {
            // Fallback to default values for testing
            weights = mapOf(
                "Amused" to doubleArrayOf(0.12, -0.33, 0.08, -0.19, 0.5, 0.3),
                "Calm" to doubleArrayOf(-0.21, 0.55, -0.07, 0.1, -0.4, -0.3),
                "Stressed" to doubleArrayOf(0.02, -0.12, 0.1, 0.05, 0.2, 0.1)
            )

            biases = mapOf(
                "Amused" to -0.2,
                "Calm" to 0.3,
                "Stressed" to 0.1
            )

            featureMeans = doubleArrayOf(72.5, 8.2, 65.0, 85.0, 45.3, 32.1)
            featureStds = doubleArrayOf(12.0, 5.5, 8.0, 15.0, 18.7, 12.4)
        }
    }

    /**
     * Predict emotion from features
     */
    fun predict(features: DoubleArray): EmotionPrediction {
        // Normalize features
        val normalized = normalizeFeatures(features)

        // Compute scores for each class
        val scores = mutableMapOf<String, Double>()
        for ((emotion, weight) in weights) {
            val score = dotProduct(normalized, weight) + (biases[emotion] ?: 0.0)
            scores[emotion] = score
        }

        // Apply softmax to get probabilities
        val probabilities = softmax(scores.values.toDoubleArray())
        val probabilityMap = scores.keys.zip(probabilities.toList()).toMap()

        // Get dominant emotion
        val dominantEmotion = scores.maxByOrNull { it.value }?.key ?: "Unknown"
        val confidence = probabilityMap[dominantEmotion] ?: 0.0

        return EmotionPrediction(
            emotion = dominantEmotion,
            confidence = confidence,
            probabilities = probabilityMap
        )
    }

    private fun normalizeFeatures(features: DoubleArray): DoubleArray {
        return features.mapIndexed { index, value ->
            (value - featureMeans[index]) / featureStds[index]
        }.toDoubleArray()
    }

    private fun dotProduct(a: DoubleArray, b: DoubleArray): Double {
        return a.zip(b).sumOf { (x, y) -> x * y }
    }

    private fun softmax(scores: DoubleArray): DoubleArray {
        val maxScore = scores.maxOrNull() ?: 0.0
        val expScores = scores.map { exp(it - maxScore) }
        val sumExp = expScores.sum()
        return expScores.map { it / sumExp }.toDoubleArray()
    }
}
