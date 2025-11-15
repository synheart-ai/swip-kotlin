package ai.synheart.swip.errors

import org.junit.Assert.*
import org.junit.Test

class SwipErrorsTest {

    @Test
    fun `test SWIPException has code and message`() {
        // Given a SWIP exception
        val exception = SWIPException("TEST_CODE", "Test message")

        // Then should have correct properties
        assertEquals("TEST_CODE", exception.code)
        assertEquals("Test message", exception.message)
        assertNull(exception.cause)
    }

    @Test
    fun `test SWIPException with cause`() {
        // Given an exception with a cause
        val cause = RuntimeException("Original error")
        val exception = SWIPException("TEST_CODE", "Wrapped error", cause)

        // Then should preserve cause
        assertEquals(cause, exception.cause)
    }

    @Test
    fun `test PermissionDeniedException has correct code`() {
        // Given a permission denied exception
        val exception = PermissionDeniedException()

        // Then should have correct code
        assertEquals("E_PERMISSION_DENIED", exception.code)
        assertTrue(exception.message.contains("permission", ignoreCase = true))
    }

    @Test
    fun `test PermissionDeniedException with custom message`() {
        // Given custom message
        val exception = PermissionDeniedException("Custom permission error")

        // Then should use custom message
        assertEquals("Custom permission error", exception.message)
        assertEquals("E_PERMISSION_DENIED", exception.code)
    }

    @Test
    fun `test InvalidConfigurationException has correct code`() {
        // Given an invalid configuration exception
        val exception = InvalidConfigurationException()

        // Then should have correct code
        assertEquals("E_INVALID_CONFIG", exception.code)
    }

    @Test
    fun `test SessionNotFoundException has correct code`() {
        // Given a session not found exception
        val exception = SessionNotFoundException()

        // Then should have correct code
        assertEquals("E_SESSION_NOT_FOUND", exception.code)
        assertTrue(exception.message.contains("session", ignoreCase = true))
    }

    @Test
    fun `test DataQualityException has correct code`() {
        // Given a data quality exception
        val exception = DataQualityException()

        // Then should have correct code
        assertEquals("E_SIGNAL_LOW_QUALITY", exception.code)
    }

    @Test
    fun `test InitializationException has correct code`() {
        // Given an initialization exception
        val exception = InitializationException()

        // Then should have correct code
        assertEquals("E_INITIALIZATION_FAILED", exception.code)
    }

    @Test
    fun `test SessionException has correct code`() {
        // Given a session exception
        val exception = SessionException()

        // Then should have correct code
        assertEquals("E_SESSION_ERROR", exception.code)
    }

    @Test
    fun `test SensorException has correct code`() {
        // Given a sensor exception
        val exception = SensorException()

        // Then should have correct code
        assertEquals("E_SENSOR_ERROR", exception.code)
    }

    @Test
    fun `test ModelException has correct code`() {
        // Given a model exception
        val exception = ModelException()

        // Then should have correct code
        assertEquals("E_MODEL_ERROR", exception.code)
    }

    @Test
    fun `test ConsentException has correct code`() {
        // Given a consent exception
        val exception = ConsentException()

        // Then should have correct code
        assertEquals("E_CONSENT_REQUIRED", exception.code)
    }

    @Test
    fun `test StorageException has correct code`() {
        // Given a storage exception
        val exception = StorageException()

        // Then should have correct code
        assertEquals("E_STORAGE_ERROR", exception.code)
    }

    @Test
    fun `test all exceptions extend SWIPException`() {
        // Given various exception instances
        val exceptions = listOf(
            PermissionDeniedException(),
            InvalidConfigurationException(),
            SessionNotFoundException(),
            DataQualityException(),
            InitializationException(),
            SessionException(),
            SensorException(),
            ModelException(),
            ConsentException(),
            StorageException()
        )

        // Then all should be SWIPException instances
        exceptions.forEach { exception ->
            assertTrue(exception is SWIPException)
            assertNotNull(exception.code)
            assertNotNull(exception.message)
        }
    }

    @Test
    fun `test exception codes are unique`() {
        // Given all exception types
        val codes = listOf(
            PermissionDeniedException().code,
            InvalidConfigurationException().code,
            SessionNotFoundException().code,
            DataQualityException().code,
            InitializationException().code,
            SessionException().code,
            SensorException().code,
            ModelException().code,
            ConsentException().code,
            StorageException().code
        )

        // Then all codes should be unique
        assertEquals(codes.size, codes.toSet().size)
    }

    @Test
    fun `test exceptions can be caught as SWIPException`() {
        // Given a function that throws specific exception
        fun throwsPermissionDenied() {
            throw PermissionDeniedException("Test error")
        }

        // When catching as SWIPException
        try {
            throwsPermissionDenied()
            fail("Should have thrown exception")
        } catch (e: SWIPException) {
            // Then should catch successfully
            assertEquals("E_PERMISSION_DENIED", e.code)
        }
    }

    @Test
    fun `test exception with cause preserves stack trace`() {
        // Given an exception with cause
        val rootCause = IllegalStateException("Root cause")
        val exception = ModelException("Model failed", rootCause)

        // Then cause should be accessible
        assertNotNull(exception.cause)
        assertEquals(rootCause, exception.cause)
        assertTrue(exception.cause is IllegalStateException)
    }

    @Test
    fun `test custom messages override defaults`() {
        // Given exceptions with custom messages
        val customMessage = "This is a custom error message"
        val exceptions = listOf(
            PermissionDeniedException(customMessage),
            InvalidConfigurationException(customMessage),
            SessionNotFoundException(customMessage),
            DataQualityException(customMessage),
            InitializationException(customMessage),
            SessionException(customMessage),
            SensorException(customMessage),
            ModelException(customMessage),
            ConsentException(customMessage),
            StorageException(customMessage)
        )

        // Then all should have custom message
        exceptions.forEach { exception ->
            assertEquals(customMessage, exception.message)
        }
    }
}

