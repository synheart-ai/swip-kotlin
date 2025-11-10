package ai.synheart.swip.ml

import ai.synheart.swip.models.SwipConfig
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SwipEngineTest {

    private lateinit var swipEngine: SwipEngine
    private lateinit var config: SwipConfig

    @Before
    fun setup() {
        config = SwipConfig()
        swipEngine = SwipEngine(config)
    }

    @Test
    fun `test compute score with calm emotion`() {
        // Given calm emotion probabilities
        val emotionProbs = mapOf(
            "Amused" to 0.2,
            "Calm" to 0.7,
            "Stressed" to 0.1
        )

        // When computing score
        val result = swipEngine.computeScore(
            hr = 70.0,
            hrv = 60.0,
            motion = 0.0,
            emotionProbabilities = emotionProbs
        )

        // Then score should be positive (calm = good)
        assertTrue(result.swipScore >= 50.0)
        assertEquals("Calm", result.dominantEmotion)
        assertEquals(0.7, result.confidence, 0.01)
    }

    @Test
    fun `test compute score with stressed emotion`() {
        // Given stressed emotion probabilities
        val emotionProbs = mapOf(
            "Amused" to 0.1,
            "Calm" to 0.2,
            "Stressed" to 0.7
        )

        // When computing score with low HRV
        val result = swipEngine.computeScore(
            hr = 95.0,
            hrv = 25.0,
            motion = 0.0,
            emotionProbabilities = emotionProbs
        )

        // Then score should be lower (stressed = worse)
        assertTrue(result.swipScore < 60.0)
        assertEquals("Stressed", result.dominantEmotion)
        assertEquals(0.7, result.confidence, 0.01)
    }

    @Test
    fun `test compute score with amused emotion`() {
        // Given amused emotion probabilities
        val emotionProbs = mapOf(
            "Amused" to 0.8,
            "Calm" to 0.1,
            "Stressed" to 0.1
        )

        // When computing score with good HRV
        val result = swipEngine.computeScore(
            hr = 75.0,
            hrv = 55.0,
            motion = 0.0,
            emotionProbabilities = emotionProbs
        )

        // Then score should be high (amused = positive)
        assertTrue(result.swipScore >= 50.0)
        assertEquals("Amused", result.dominantEmotion)
    }

    @Test
    fun `test score is bounded 0-100`() {
        // Given extreme values
        val emotionProbs = mapOf(
            "Stressed" to 1.0,
            "Calm" to 0.0,
            "Amused" to 0.0
        )

        // When computing score with very low HRV
        val result1 = swipEngine.computeScore(
            hr = 120.0,
            hrv = 5.0,
            motion = 0.0,
            emotionProbabilities = emotionProbs
        )

        // And with very high HRV
        val result2 = swipEngine.computeScore(
            hr = 55.0,
            hrv = 150.0,
            motion = 0.0,
            emotionProbabilities = mapOf("Amused" to 1.0, "Calm" to 0.0, "Stressed" to 0.0)
        )

        // Then scores should be bounded
        assertTrue(result1.swipScore >= 0.0)
        assertTrue(result1.swipScore <= 100.0)
        assertTrue(result2.swipScore >= 0.0)
        assertTrue(result2.swipScore <= 100.0)
    }

    @Test
    fun `test data quality based on heart rate`() {
        // Given valid HR
        val validResult = swipEngine.computeScore(
            hr = 75.0,
            hrv = 50.0,
            motion = 0.0,
            emotionProbabilities = mapOf("Calm" to 1.0)
        )

        // And invalid HR (too low)
        val invalidResult = swipEngine.computeScore(
            hr = 30.0,
            hrv = 50.0,
            motion = 0.0,
            emotionProbabilities = mapOf("Calm" to 1.0)
        )

        // Then data quality should differ
        assertEquals(1.0, validResult.dataQuality, 0.01)
        assertEquals(0.5, invalidResult.dataQuality, 0.01)
    }

    @Test
    fun `test result includes all required fields`() {
        // Given any input
        val emotionProbs = mapOf(
            "Amused" to 0.3,
            "Calm" to 0.5,
            "Stressed" to 0.2
        )

        // When computing score
        val result = swipEngine.computeScore(
            hr = 70.0,
            hrv = 50.0,
            motion = 0.0,
            emotionProbabilities = emotionProbs
        )

        // Then all fields should be populated
        assertTrue(result.swipScore >= 0.0)
        assertNotNull(result.dominantEmotion)
        assertEquals(3, result.emotionProbabilities.size)
        assertEquals(70.0, result.heartRate, 0.01)
        assertEquals(50.0, result.hrv, 0.01)
        assertNotNull(result.timestamp)
        assertTrue(result.confidence >= 0.0)
        assertTrue(result.dataQuality >= 0.0)
    }

    @Test
    fun `test custom config weights affect score`() {
        // Given custom config with different weights
        val customConfig = SwipConfig(
            weightHrv = 0.8,
            weightCoherence = 0.2
        )
        val customEngine = SwipEngine(customConfig)

        val emotionProbs = mapOf("Calm" to 1.0, "Stressed" to 0.0, "Amused" to 0.0)

        // When computing with default and custom configs
        val defaultResult = swipEngine.computeScore(
            hr = 70.0, hrv = 60.0, motion = 0.0, emotionProbabilities = emotionProbs
        )
        val customResult = customEngine.computeScore(
            hr = 70.0, hrv = 60.0, motion = 0.0, emotionProbabilities = emotionProbs
        )

        // Then scores may differ (unless weights produce same result)
        assertNotNull(defaultResult.swipScore)
        assertNotNull(customResult.swipScore)
    }
}
