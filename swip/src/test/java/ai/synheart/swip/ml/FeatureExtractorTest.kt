package ai.synheart.swip.ml

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.Instant

class FeatureExtractorTest {

    private lateinit var featureExtractor: FeatureExtractor

    @Before
    fun setup() {
        featureExtractor = FeatureExtractor()
    }

    @Test
    fun `test extract features from empty window`() {
        // Given an empty data window
        val emptyWindow = emptyList<DataPoint>()

        // When extracting features
        val features = featureExtractor.extract(emptyWindow)

        // Then should return zero-filled array
        assertEquals(6, features.size)
        assertTrue(features.all { it == 0.0 })
    }

    @Test
    fun `test extract features from single data point`() {
        // Given a single data point
        val window = listOf(
            DataPoint(hr = 75.0, hrv = 50.0, timestamp = Instant.now(), motion = 0.0)
        )

        // When extracting features
        val features = featureExtractor.extract(window)

        // Then should return valid features
        assertEquals(6, features.size)
        assertEquals(75.0, features[0], 0.01) // Mean HR
        assertEquals(0.0, features[1], 0.01)  // Std HR (single point = 0)
        assertEquals(75.0, features[2], 0.01) // Min HR
        assertEquals(75.0, features[3], 0.01) // Max HR
    }

    @Test
    fun `test extract features from normal window`() {
        // Given a window of data points with varying HR/HRV
        val window = listOf(
            DataPoint(hr = 70.0, hrv = 45.0, timestamp = Instant.now(), motion = 0.0),
            DataPoint(hr = 75.0, hrv = 50.0, timestamp = Instant.now(), motion = 0.0),
            DataPoint(hr = 80.0, hrv = 55.0, timestamp = Instant.now(), motion = 0.0),
            DataPoint(hr = 85.0, hrv = 60.0, timestamp = Instant.now(), motion = 0.0),
            DataPoint(hr = 90.0, hrv = 65.0, timestamp = Instant.now(), motion = 0.0)
        )

        // When extracting features
        val features = featureExtractor.extract(window)

        // Then should return correct statistics
        assertEquals(6, features.size)
        assertEquals(80.0, features[0], 0.01)  // Mean HR
        assertTrue(features[1] > 0)            // Std HR > 0
        assertEquals(70.0, features[2], 0.01)  // Min HR
        assertEquals(90.0, features[3], 0.01)  // Max HR
        assertEquals(55.0, features[4], 0.01)  // SDNN (mean HRV)
        assertTrue(features[5] > 0)            // RMSSD > 0
    }

    @Test
    fun `test feature array size is consistent`() {
        // Given windows of different sizes
        val smallWindow = listOf(
            DataPoint(hr = 75.0, hrv = 50.0, timestamp = Instant.now(), motion = 0.0)
        )
        val largeWindow = (1..100).map {
            DataPoint(hr = 70.0 + it, hrv = 40.0 + it, timestamp = Instant.now(), motion = 0.0)
        }

        // When extracting features
        val smallFeatures = featureExtractor.extract(smallWindow)
        val largeFeatures = featureExtractor.extract(largeWindow)

        // Then feature array size should be consistent
        assertEquals(6, smallFeatures.size)
        assertEquals(6, largeFeatures.size)
    }

    @Test
    fun `test RMSSD calculation with consecutive values`() {
        // Given data points with known differences
        val window = listOf(
            DataPoint(hr = 75.0, hrv = 40.0, timestamp = Instant.now(), motion = 0.0),
            DataPoint(hr = 75.0, hrv = 50.0, timestamp = Instant.now(), motion = 0.0),
            DataPoint(hr = 75.0, hrv = 60.0, timestamp = Instant.now(), motion = 0.0)
        )

        // When extracting features
        val features = featureExtractor.extract(window)

        // Then RMSSD should be non-zero
        assertTrue(features[5] > 0) // RMSSD index
    }
}
