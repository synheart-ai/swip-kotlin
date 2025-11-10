package ai.synheart.swip.ml

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SvmPredictorTest {

    private lateinit var svmPredictor: SvmPredictor

    @Before
    fun setup() {
        svmPredictor = SvmPredictor()
    }

    @Test
    fun `test predict with zero features`() {
        // Given zero features
        val features = doubleArrayOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)

        // When predicting
        val prediction = svmPredictor.predict(features)

        // Then should return valid prediction
        assertNotNull(prediction.emotion)
        assertTrue(prediction.confidence >= 0.0 && prediction.confidence <= 1.0)
        assertEquals(3, prediction.probabilities.size) // 3 emotion classes

        // Probabilities should sum to ~1.0
        val probSum = prediction.probabilities.values.sum()
        assertEquals(1.0, probSum, 0.01)
    }

    @Test
    fun `test predict with normal HR features`() {
        // Given features for normal resting state
        // Mean HR=70, Std=5, Min=65, Max=75, SDNN=50, RMSSD=40
        val features = doubleArrayOf(70.0, 5.0, 65.0, 75.0, 50.0, 40.0)

        // When predicting
        val prediction = svmPredictor.predict(features)

        // Then should return valid prediction
        assertNotNull(prediction.emotion)
        assertTrue(prediction.confidence > 0.0)
        assertTrue(listOf("Amused", "Calm", "Stressed").contains(prediction.emotion))
    }

    @Test
    fun `test predict with stressed features`() {
        // Given features indicating stress
        // High HR, low HRV
        val features = doubleArrayOf(100.0, 15.0, 85.0, 115.0, 20.0, 15.0)

        // When predicting
        val prediction = svmPredictor.predict(features)

        // Then should return valid prediction
        assertNotNull(prediction.emotion)
        assertTrue(prediction.confidence >= 0.0)
    }

    @Test
    fun `test predict with calm features`() {
        // Given features indicating calm state
        // Low HR, high HRV
        val features = doubleArrayOf(60.0, 5.0, 55.0, 65.0, 70.0, 60.0)

        // When predicting
        val prediction = svmPredictor.predict(features)

        // Then should return valid prediction
        assertNotNull(prediction.emotion)
        assertTrue(prediction.confidence >= 0.0)
    }

    @Test
    fun `test probabilities are valid`() {
        // Given any features
        val features = doubleArrayOf(75.0, 10.0, 60.0, 90.0, 50.0, 40.0)

        // When predicting
        val prediction = svmPredictor.predict(features)

        // Then all probabilities should be valid
        prediction.probabilities.forEach { (emotion, prob) ->
            assertTrue("$emotion probability should be >= 0", prob >= 0.0)
            assertTrue("$emotion probability should be <= 1", prob <= 1.0)
        }

        // And probabilities should sum to 1
        val sum = prediction.probabilities.values.sum()
        assertEquals(1.0, sum, 0.01)
    }

    @Test
    fun `test dominant emotion matches highest probability`() {
        // Given any features
        val features = doubleArrayOf(75.0, 10.0, 60.0, 90.0, 50.0, 40.0)

        // When predicting
        val prediction = svmPredictor.predict(features)

        // Then dominant emotion should have highest probability
        val maxProb = prediction.probabilities.maxByOrNull { it.value }
        assertEquals(prediction.emotion, maxProb?.key)
        assertEquals(prediction.confidence, maxProb?.value ?: 0.0, 0.001)
    }

    @Test
    fun `test prediction is deterministic`() {
        // Given the same features
        val features = doubleArrayOf(75.0, 10.0, 60.0, 90.0, 50.0, 40.0)

        // When predicting multiple times
        val prediction1 = svmPredictor.predict(features)
        val prediction2 = svmPredictor.predict(features)

        // Then results should be identical
        assertEquals(prediction1.emotion, prediction2.emotion)
        assertEquals(prediction1.confidence, prediction2.confidence, 0.0001)
    }
}
