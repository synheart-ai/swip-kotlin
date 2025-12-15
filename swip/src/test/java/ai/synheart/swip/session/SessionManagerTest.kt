package ai.synheart.swip.session

import ai.synheart.swip.models.SessionState
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SessionManagerTest {

    private lateinit var sessionManager: SessionManager

    @Before
    fun setup() {
        sessionManager = SessionManager()
    }

    @Test
    fun `test start session creates new session`() {
        // When starting a session
        val session = sessionManager.startSession(
            sessionId = "test-123",
            appId = "com.example.app",
            metadata = mapOf("screen" to "home")
        )

        // Then session should be created
        assertEquals("test-123", session.id)
        assertEquals("com.example.app", session.appId)
        assertEquals(SessionState.ACTIVE, session.state)
    }

    @Test
    fun `test get session returns existing session`() {
        // Given a started session
        sessionManager.startSession(
            sessionId = "test-123",
            appId = "com.example.app",
            metadata = emptyMap()
        )

        // When retrieving the session
        val session = sessionManager.getSession("test-123")

        // Then session should be returned
        assertNotNull(session)
        assertEquals("test-123", session?.id)
    }

    @Test
    fun `test get session returns null for non-existent session`() {
        // When retrieving a non-existent session
        val session = sessionManager.getSession("non-existent")

        // Then should return null
        assertNull(session)
    }

    @Test
    fun `test end session updates state`() {
        // Given an active session
        sessionManager.startSession(
            sessionId = "test-123",
            appId = "com.example.app",
            metadata = emptyMap()
        )

        // When ending the session
        sessionManager.endSession("test-123")

        // Then session state should be ENDED
        val session = sessionManager.getSession("test-123")
        assertEquals(SessionState.ENDED, session?.state)
        assertNotNull(session?.endTime)
    }

    @Test
    fun `test get active sessions returns only active sessions`() {
        // Given multiple sessions
        sessionManager.startSession("session-1", "app1", emptyMap())
        sessionManager.startSession("session-2", "app2", emptyMap())
        sessionManager.endSession("session-1")

        // When getting active sessions
        val activeSessions = sessionManager.getActiveSessions()

        // Then only active session should be returned
        assertEquals(1, activeSessions.size)
        assertEquals("session-2", activeSessions[0].id)
    }

    @Test
    fun `test purge all data clears sessions`() {
        // Given multiple sessions
        sessionManager.startSession("session-1", "app1", emptyMap())
        sessionManager.startSession("session-2", "app2", emptyMap())

        // When purging all data
        sessionManager.purgeAllData()

        // Then all sessions should be cleared
        assertNull(sessionManager.getSession("session-1"))
        assertNull(sessionManager.getSession("session-2"))
        assertTrue(sessionManager.getActiveSessions().isEmpty())
    }

    @Test
    fun `test session metadata is preserved`() {
        // Given a session with metadata
        val metadata = mapOf(
            "screen" to "meditation",
            "duration_minutes" to 10,
            "user_id" to "user-123"
        )

        sessionManager.startSession(
            sessionId = "test-123",
            appId = "com.example.app",
            metadata = metadata
        )

        // When retrieving the session
        val session = sessionManager.getSession("test-123")

        // Then metadata should be preserved
        assertEquals("meditation", session?.metadata?.get("screen"))
        assertEquals(10, session?.metadata?.get("duration_minutes"))
        assertEquals("user-123", session?.metadata?.get("user_id"))
    }

    @Test
    fun `test concurrent sessions are supported`() {
        // When starting multiple sessions
        sessionManager.startSession("session-1", "app1", emptyMap())
        sessionManager.startSession("session-2", "app2", emptyMap())
        sessionManager.startSession("session-3", "app3", emptyMap())

        // Then all should be active
        val activeSessions = sessionManager.getActiveSessions()
        assertEquals(3, activeSessions.size)
    }
}
