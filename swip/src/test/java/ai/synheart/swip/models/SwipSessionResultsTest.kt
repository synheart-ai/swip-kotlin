package ai.synheart.swip.models

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class SwipSessionResultsTest {

    @Test
    fun `test session results with scores`() {
        // Given session results with scores
        val startTime = Instant.now().minus(10, ChronoUnit.MINUTES)
        val endTime = Instant.now()
        val scores = listOf(
            SwipScoreResult(
                swipScore = 75.0,
                dominantEmotion = "Calm",
                emotionProbabilities = mapOf("Calm" to 0.8),
                hrv = 55.0,
                heartRate = 70.0,
                timestamp = startTime,
                confidence = 0.8,
                dataQuality = 1.0
            ),
            SwipScoreResult(
                swipScore = 80.0,
                dominantEmotion = "Amused",
                emotionProbabilities = mapOf("Amused" to 0.9),
                hrv = 60.0,
                heartRate = 72.0,
                timestamp = startTime.plusSeconds(60),
                confidence = 0.9,
                dataQuality = 1.0
            )
        )
        val emotions = listOf(
            EmotionResult(
                emotion = "Calm",
                confidence = 0.8,
                probabilities = mapOf("Calm" to 0.8),
                timestamp = startTime
            )
        )

        val sessionResults = SwipSessionResults(
            sessionId = "test-123",
            scores = scores,
            emotions = emotions,
            startTime = startTime,
            endTime = endTime
        )

        // Then session should have correct properties
        assertEquals("test-123", sessionResults.sessionId)
        assertEquals(2, sessionResults.scores.size)
        assertEquals(1, sessionResults.emotions.size)
        assertEquals(startTime, sessionResults.startTime)
        assertEquals(endTime, sessionResults.endTime)
    }

    @Test
    fun `test get summary with valid data`() {
        // Given session with scores
        val startTime = Instant.now().minus(5, ChronoUnit.MINUTES)
        val endTime = Instant.now()
        val scores = listOf(
            SwipScoreResult(70.0, "Calm", emptyMap(), 50.0, 70.0, startTime),
            SwipScoreResult(80.0, "Calm", emptyMap(), 55.0, 72.0, startTime.plusSeconds(30)),
            SwipScoreResult(90.0, "Amused", emptyMap(), 60.0, 75.0, startTime.plusSeconds(60))
        )

        val sessionResults = SwipSessionResults(
            sessionId = "test-123",
            scores = scores,
            emotions = emptyList(),
            startTime = startTime,
            endTime = endTime
        )

        // When getting summary
        val summary = sessionResults.getSummary()

        // Then summary should contain expected values
        assertEquals("test-123", summary["session_id"])
        assertEquals(80.0, summary["average_swip_score"] as Double, 0.01)
        assertEquals(3, summary["score_count"])
        assertEquals(0, summary["emotion_count"])
        assertTrue((summary["duration_seconds"] as Long) > 0)
    }

    @Test
    fun `test get summary with empty scores`() {
        // Given session with no scores
        val startTime = Instant.now()
        val endTime = Instant.now()
        val sessionResults = SwipSessionResults(
            sessionId = "empty-session",
            scores = emptyList(),
            emotions = emptyList(),
            startTime = startTime,
            endTime = endTime
        )

        // When getting summary
        val summary = sessionResults.getSummary()

        // Then should return default values
        assertEquals(0, summary["duration_seconds"])
        assertEquals(0.0, summary["average_swip_score"])
        assertEquals("Unknown", summary["dominant_emotion"])
    }

    @Test
    fun `test dominant emotion is most frequent`() {
        // Given scores with different emotions
        val startTime = Instant.now()
        val scores = listOf(
            SwipScoreResult(70.0, "Calm", emptyMap(), 50.0, 70.0, startTime),
            SwipScoreResult(75.0, "Calm", emptyMap(), 52.0, 71.0, startTime.plusSeconds(10)),
            SwipScoreResult(80.0, "Calm", emptyMap(), 54.0, 72.0, startTime.plusSeconds(20)),
            SwipScoreResult(85.0, "Amused", emptyMap(), 56.0, 73.0, startTime.plusSeconds(30)),
            SwipScoreResult(65.0, "Stressed", emptyMap(), 48.0, 75.0, startTime.plusSeconds(40))
        )

        val sessionResults = SwipSessionResults(
            sessionId = "test-123",
            scores = scores,
            emotions = emptyList(),
            startTime = startTime,
            endTime = startTime.plusSeconds(50)
        )

        // When getting summary
        val summary = sessionResults.getSummary()

        // Then Calm should be dominant (appears 3 times)
        assertEquals("Calm", summary["dominant_emotion"])
    }

    @Test
    fun `test average score calculation`() {
        // Given scores with known values
        val startTime = Instant.now()
        val scores = listOf(
            SwipScoreResult(50.0, "Stressed", emptyMap(), 40.0, 80.0, startTime),
            SwipScoreResult(70.0, "Calm", emptyMap(), 50.0, 72.0, startTime.plusSeconds(10)),
            SwipScoreResult(90.0, "Amused", emptyMap(), 60.0, 68.0, startTime.plusSeconds(20))
        )

        val sessionResults = SwipSessionResults(
            sessionId = "test-123",
            scores = scores,
            emotions = emptyList(),
            startTime = startTime,
            endTime = startTime.plusSeconds(30)
        )

        // When getting summary
        val summary = sessionResults.getSummary()

        // Then average should be 70.0 (mean of 50, 70, 90)
        assertEquals(70.0, summary["average_swip_score"] as Double, 0.01)
    }

    @Test
    fun `test duration calculation`() {
        // Given session with known duration
        val startTime = Instant.parse("2024-01-01T10:00:00Z")
        val endTime = Instant.parse("2024-01-01T10:05:00Z") // 5 minutes later

        val scores = listOf(
            SwipScoreResult(75.0, "Calm", emptyMap(), 50.0, 70.0, startTime)
        )

        val sessionResults = SwipSessionResults(
            sessionId = "test-123",
            scores = scores,
            emotions = emptyList(),
            startTime = startTime,
            endTime = endTime
        )

        // When getting summary
        val summary = sessionResults.getSummary()

        // Then duration should be 300 seconds (5 minutes)
        assertEquals(300L, summary["duration_seconds"])
    }

    @Test
    fun `test session with multiple emotion types`() {
        // Given varied emotions
        val startTime = Instant.now()
        val emotions = listOf(
            EmotionResult("Calm", 0.8, mapOf("Calm" to 0.8), startTime),
            EmotionResult("Amused", 0.7, mapOf("Amused" to 0.7), startTime.plusSeconds(30)),
            EmotionResult("Stressed", 0.6, mapOf("Stressed" to 0.6), startTime.plusSeconds(60))
        )

        val sessionResults = SwipSessionResults(
            sessionId = "test-123",
            scores = emptyList(),
            emotions = emotions,
            startTime = startTime,
            endTime = startTime.plusSeconds(90)
        )

        // Then should have all emotions
        assertEquals(3, sessionResults.emotions.size)
    }

    @Test
    fun `test SwipScoreResult properties`() {
        // Given a score result
        val timestamp = Instant.now()
        val result = SwipScoreResult(
            swipScore = 85.5,
            dominantEmotion = "Amused",
            emotionProbabilities = mapOf("Amused" to 0.9, "Calm" to 0.1),
            hrv = 62.5,
            heartRate = 73.2,
            timestamp = timestamp,
            confidence = 0.87,
            dataQuality = 0.95
        )

        // Then all properties should be set correctly
        assertEquals(85.5, result.swipScore, 0.01)
        assertEquals("Amused", result.dominantEmotion)
        assertEquals(2, result.emotionProbabilities.size)
        assertEquals(62.5, result.hrv, 0.01)
        assertEquals(73.2, result.heartRate, 0.01)
        assertEquals(timestamp, result.timestamp)
        assertEquals(0.87, result.confidence, 0.01)
        assertEquals(0.95, result.dataQuality, 0.01)
    }

    @Test
    fun `test EmotionResult properties`() {
        // Given an emotion result
        val timestamp = Instant.now()
        val probabilities = mapOf("Calm" to 0.8, "Amused" to 0.15, "Stressed" to 0.05)
        val result = EmotionResult(
            emotion = "Calm",
            confidence = 0.8,
            probabilities = probabilities,
            timestamp = timestamp
        )

        // Then all properties should be set correctly
        assertEquals("Calm", result.emotion)
        assertEquals(0.8, result.confidence, 0.01)
        assertEquals(3, result.probabilities.size)
        assertEquals(timestamp, result.timestamp)
    }

    @Test
    fun `test session with tie in emotion counts`() {
        // Given equal emotion counts
        val startTime = Instant.now()
        val scores = listOf(
            SwipScoreResult(70.0, "Calm", emptyMap(), 50.0, 70.0, startTime),
            SwipScoreResult(75.0, "Amused", emptyMap(), 52.0, 71.0, startTime.plusSeconds(10))
        )

        val sessionResults = SwipSessionResults(
            sessionId = "test-123",
            scores = scores,
            emotions = emptyList(),
            startTime = startTime,
            endTime = startTime.plusSeconds(20)
        )

        // When getting summary
        val summary = sessionResults.getSummary()

        // Then should return one of the emotions (implementation specific)
        val dominantEmotion = summary["dominant_emotion"] as String
        assertTrue(dominantEmotion == "Calm" || dominantEmotion == "Amused")
    }

    @Test
    fun `test session with single score`() {
        // Given session with single score
        val startTime = Instant.now()
        val endTime = startTime.plusSeconds(60)
        val scores = listOf(
            SwipScoreResult(75.0, "Calm", emptyMap(), 50.0, 70.0, startTime)
        )

        val sessionResults = SwipSessionResults(
            sessionId = "single",
            scores = scores,
            emotions = emptyList(),
            startTime = startTime,
            endTime = endTime
        )

        // When getting summary
        val summary = sessionResults.getSummary()

        // Then should handle single score correctly
        assertEquals(75.0, summary["average_swip_score"] as Double, 0.01)
        assertEquals("Calm", summary["dominant_emotion"])
        assertEquals(1, summary["score_count"])
    }

    @Test
    fun `test score result with default confidence and data quality`() {
        // Given score result with default values
        val timestamp = Instant.now()
        val result = SwipScoreResult(
            swipScore = 75.0,
            dominantEmotion = "Calm",
            emotionProbabilities = emptyMap(),
            hrv = 50.0,
            heartRate = 70.0,
            timestamp = timestamp
        )

        // Then defaults should be applied
        assertEquals(0.0, result.confidence, 0.01)
        assertEquals(1.0, result.dataQuality, 0.01)
    }
}

