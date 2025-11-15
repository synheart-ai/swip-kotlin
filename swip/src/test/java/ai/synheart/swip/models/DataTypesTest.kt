package ai.synheart.swip.models

import org.junit.Assert.*
import org.junit.Test

class DataTypesTest {

    @Test
    fun `test ConsentLevel allows hierarchy`() {
        // ON_DEVICE allows only itself
        assertTrue(ConsentLevel.ON_DEVICE.allows(ConsentLevel.ON_DEVICE))
        assertFalse(ConsentLevel.ON_DEVICE.allows(ConsentLevel.LOCAL_EXPORT))
        assertFalse(ConsentLevel.ON_DEVICE.allows(ConsentLevel.DASHBOARD_SHARE))

        // LOCAL_EXPORT allows itself and ON_DEVICE
        assertTrue(ConsentLevel.LOCAL_EXPORT.allows(ConsentLevel.ON_DEVICE))
        assertTrue(ConsentLevel.LOCAL_EXPORT.allows(ConsentLevel.LOCAL_EXPORT))
        assertFalse(ConsentLevel.LOCAL_EXPORT.allows(ConsentLevel.DASHBOARD_SHARE))

        // DASHBOARD_SHARE allows all
        assertTrue(ConsentLevel.DASHBOARD_SHARE.allows(ConsentLevel.ON_DEVICE))
        assertTrue(ConsentLevel.DASHBOARD_SHARE.allows(ConsentLevel.LOCAL_EXPORT))
        assertTrue(ConsentLevel.DASHBOARD_SHARE.allows(ConsentLevel.DASHBOARD_SHARE))
    }

    @Test
    fun `test SwipScoreRange contains correct ranges`() {
        assertTrue(SwipScoreRange.POSITIVE.contains(90.0))
        assertTrue(SwipScoreRange.NEUTRAL.contains(65.0))
        assertTrue(SwipScoreRange.MILD_STRESS.contains(45.0))
        assertTrue(SwipScoreRange.NEGATIVE.contains(25.0))

        assertFalse(SwipScoreRange.POSITIVE.contains(70.0))
        assertFalse(SwipScoreRange.NEUTRAL.contains(85.0))
    }

    @Test
    fun `test SwipScoreRange forScore classification`() {
        assertEquals(SwipScoreRange.POSITIVE, SwipScoreRange.forScore(85.0))
        assertEquals(SwipScoreRange.NEUTRAL, SwipScoreRange.forScore(70.0))
        assertEquals(SwipScoreRange.MILD_STRESS, SwipScoreRange.forScore(50.0))
        assertEquals(SwipScoreRange.NEGATIVE, SwipScoreRange.forScore(30.0))
    }

    @Test
    fun `test EmotionClass has correct utilities`() {
        assertEquals(0.95, EmotionClass.AMUSED.utility, 0.01)
        assertEquals(0.85, EmotionClass.CALM.utility, 0.01)
        assertEquals(0.80, EmotionClass.FOCUSED.utility, 0.01)
        assertEquals(0.70, EmotionClass.NEUTRAL.utility, 0.01)
        assertEquals(0.15, EmotionClass.STRESSED.utility, 0.01)
    }

    @Test
    fun `test EmotionClass fromLabel is case insensitive`() {
        assertEquals(EmotionClass.AMUSED, EmotionClass.fromLabel("amused"))
        assertEquals(EmotionClass.AMUSED, EmotionClass.fromLabel("Amused"))
        assertEquals(EmotionClass.AMUSED, EmotionClass.fromLabel("AMUSED"))
        assertEquals(EmotionClass.CALM, EmotionClass.fromLabel("calm"))
        assertNull(EmotionClass.fromLabel("unknown"))
    }

    @Test
    fun `test SessionState flags`() {
        assertTrue(SessionState.ACTIVE.isActive)
        assertFalse(SessionState.IDLE.isActive)

        assertTrue(SessionState.IDLE.canStart)
        assertTrue(SessionState.ENDED.canStart)
        assertFalse(SessionState.ACTIVE.canStart)

        assertTrue(SessionState.ACTIVE.canStop)
        assertTrue(SessionState.STARTING.canStop)
        assertFalse(SessionState.IDLE.canStop)
    }

    @Test
    fun `test DataQuality classification`() {
        assertEquals(DataQuality.HIGH, DataQuality.forScore(0.9))
        assertEquals(DataQuality.MEDIUM, DataQuality.forScore(0.5))
        assertEquals(DataQuality.LOW, DataQuality.forScore(0.2))

        assertTrue(DataQuality.HIGH.isAcceptable)
        assertTrue(DataQuality.MEDIUM.isAcceptable)
        assertFalse(DataQuality.LOW.isAcceptable)
    }

    @Test
    fun `test ConsentRecord JSON serialization`() {
        val record = ConsentRecord(
            level = ConsentLevel.LOCAL_EXPORT,
            grantedAt = java.time.Instant.parse("2024-01-01T00:00:00Z"),
            reason = "Testing"
        )

        val json = record.toJson()
        assertEquals(1, json["level"])
        assertEquals("LOCAL_EXPORT", json["level_name"])
        assertEquals("2024-01-01T00:00:00Z", json["granted_at"])
        assertEquals("Testing", json["reason"])

        val restored = ConsentRecord.fromJson(json)
        assertEquals(record.level, restored.level)
        assertEquals(record.reason, restored.reason)
    }
}
