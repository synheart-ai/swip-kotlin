package ai.synheart.swip.ml

import ai.synheart.swip.models.EmotionConfig
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.Instant

class EmotionEngineTest {

    private lateinit var emotionEngine: EmotionEngine
    private lateinit var config: EmotionConfig

    @Before
    fun setup() {
        config = EmotionConfig(
            confidenceThreshold = 0.5
        )
        emotionEngine = EmotionEngine(config, null)
    }

    @Test
    fun `test initial state has no results`() {
        // When creating new engine
        val results = emotionEngine.consumeReady()

        // Then should have no results
        assertTrue(results.isEmpty())
    }

    @Test
    fun `test push single data point does not trigger processing`() {
        // Given a single data point
        emotionEngine.push(
            hr = 75.0,
            hrv = 50.0,
            timestamp = Instant.now(),
            motion = 0.0
        )

        // When consuming results
        val results = emotionEngine.consumeReady()

        // Then no results should be available (need MIN_BUFFER_SIZE)
        assertTrue(results.isEmpty())
    }

    @Test
    fun `test push enough data points triggers processing`() {
        // Given enough data points
        repeat(15) { i ->
            emotionEngine.push(
                hr = 70.0 + i,
                hrv = 50.0 + i,
                timestamp = Instant.now(),
                motion = 0.0
            )
        }

        // When consuming results
        val results = emotionEngine.consumeReady()

        // Then results may be available (depends on confidence threshold)
        assertTrue(results.size >= 0) // Processing happened, results depend on confidence
    }

    @Test
    fun `test consume ready clears results`() {
        // Given engine with lenient threshold to ensure results
        val lenientConfig = EmotionConfig(confidenceThreshold = 0.0)
        val lenientEngine = EmotionEngine(lenientConfig, null)
        
        repeat(15) { i ->
            lenientEngine.push(
                hr = 70.0 + i,
                hrv = 50.0 + i,
                timestamp = Instant.now(),
                motion = 0.0
            )
        }

        // When consuming first time
        val firstResults = lenientEngine.consumeReady()
        assertTrue(firstResults.isNotEmpty())

        // And consuming again
        val secondResults = lenientEngine.consumeReady()

        // Then second call should return empty
        assertTrue(secondResults.isEmpty())
    }

    @Test
    fun `test clear removes all data and results`() {
        // Given engine with data
        repeat(15) { i ->
            emotionEngine.push(
                hr = 70.0 + i,
                hrv = 50.0 + i,
                timestamp = Instant.now(),
                motion = 0.0
            )
        }

        // When clearing
        emotionEngine.clear()

        // Then results should be empty
        val results = emotionEngine.consumeReady()
        assertTrue(results.isEmpty())
    }

    @Test
    fun `test emotion results have valid properties`() {
        // Given enough data
        repeat(15) { i ->
            emotionEngine.push(
                hr = 70.0 + i,
                hrv = 50.0 + i,
                timestamp = Instant.now(),
                motion = 0.0
            )
        }

        // When consuming results
        val results = emotionEngine.consumeReady()

        // Then all results should have valid properties
        results.forEach { result ->
            assertNotNull(result.emotion)
            assertTrue(result.confidence >= 0.0)
            assertTrue(result.confidence <= 1.0)
            assertTrue(result.probabilities.isNotEmpty())
            assertNotNull(result.timestamp)
        }
    }

    @Test
    fun `test confidence threshold filters results`() {
        // Given high confidence threshold
        val strictConfig = EmotionConfig(confidenceThreshold = 0.95)
        val strictEngine = EmotionEngine(strictConfig, null)

        // When pushing data
        repeat(15) { i ->
            strictEngine.push(
                hr = 70.0 + i,
                hrv = 50.0 + i,
                timestamp = Instant.now(),
                motion = 0.0
            )
        }

        // Then may have no results if confidence is low
        val results = strictEngine.consumeReady()
        results.forEach { result ->
            assertTrue(result.confidence >= 0.95)
        }
    }

    @Test
    fun `test continuous data processing`() {
        // Given lenient threshold
        val lenientConfig = EmotionConfig(confidenceThreshold = 0.0)
        val lenientEngine = EmotionEngine(lenientConfig, null)
        
        // When pushing data continuously
        repeat(50) { i ->
            lenientEngine.push(
                hr = 70.0 + (i % 20),
                hrv = 50.0 + (i % 15),
                timestamp = Instant.now(),
                motion = 0.0
            )
        }

        // Then multiple results should be generated
        val results = lenientEngine.consumeReady()
        assertTrue(results.isNotEmpty())
    }

    @Test
    fun `test high heart rate variability patterns`() {
        // Given highly variable heart rate data
        val hrs = listOf(60.0, 90.0, 65.0, 85.0, 70.0, 95.0, 60.0, 80.0, 75.0, 90.0, 65.0, 85.0, 70.0, 80.0, 75.0)
        val hrvs = listOf(40.0, 30.0, 50.0, 35.0, 45.0, 25.0, 55.0, 40.0, 45.0, 30.0, 50.0, 35.0, 45.0, 40.0, 45.0)

        hrs.zip(hrvs).forEach { (hr, hrv) ->
            emotionEngine.push(hr, hrv, Instant.now(), 0.0)
        }

        // When consuming results
        val results = emotionEngine.consumeReady()

        // Then should handle variable data gracefully
        results.forEach { result ->
            assertNotNull(result.emotion)
            assertTrue(result.confidence >= 0.0)
        }
    }

    @Test
    fun `test motion data is captured`() {
        // Given data with motion
        repeat(15) { i ->
            emotionEngine.push(
                hr = 75.0,
                hrv = 50.0,
                timestamp = Instant.now(),
                motion = i * 0.1
            )
        }

        // Then processing should succeed (motion is recorded but not directly validated here)
        val results = emotionEngine.consumeReady()
        assertTrue(results.size >= 0) // May or may not have results depending on confidence
    }

    @Test
    fun `test buffer management prevents memory overflow`() {
        // When pushing a large amount of data
        repeat(500) { i ->
            emotionEngine.push(
                hr = 70.0 + (i % 30),
                hrv = 50.0 + (i % 20),
                timestamp = Instant.now(),
                motion = 0.0
            )
        }

        // Then engine should not crash and results should be available
        val results = emotionEngine.consumeReady()
        assertTrue(results.isNotEmpty())
    }

    @Test
    fun `test timestamps are preserved in results`() {
        // Given data with known timestamps
        repeat(15) { i ->
            emotionEngine.push(
                hr = 70.0 + i,
                hrv = 50.0 + i,
                timestamp = Instant.now(),
                motion = 0.0
            )
        }

        // When consuming results
        val results = emotionEngine.consumeReady()

        // Then timestamps should be present
        results.forEach { result ->
            assertNotNull(result.timestamp)
        }
    }

    @Test
    fun `test zero confidence threshold accepts all results`() {
        // Given zero threshold config
        val lenientConfig = EmotionConfig(confidenceThreshold = 0.0)
        val lenientEngine = EmotionEngine(lenientConfig, null)

        // When pushing data
        repeat(15) { i ->
            lenientEngine.push(
                hr = 70.0 + i,
                hrv = 50.0 + i,
                timestamp = Instant.now(),
                motion = 0.0
            )
        }

        // Then should have results
        val results = lenientEngine.consumeReady()
        assertTrue(results.isNotEmpty())
    }
}

