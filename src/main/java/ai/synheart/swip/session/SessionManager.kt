package ai.synheart.swip.session

import ai.synheart.swip.models.SessionState
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages SWIP sessions
 */
class SessionManager {
    private val sessions = ConcurrentHashMap<String, Session>()

    /**
     * Start a new session
     */
    fun startSession(
        sessionId: String,
        appId: String,
        metadata: Map<String, Any>
    ): Session {
        val session = Session(
            id = sessionId,
            appId = appId,
            metadata = metadata,
            startTime = Instant.now(),
            state = SessionState.ACTIVE
        )
        sessions[sessionId] = session
        return session
    }

    /**
     * End a session
     */
    fun endSession(sessionId: String) {
        sessions[sessionId]?.let { session ->
            sessions[sessionId] = session.copy(
                state = SessionState.ENDED,
                endTime = Instant.now()
            )
        }
    }

    /**
     * Get a session by ID
     */
    fun getSession(sessionId: String): Session? = sessions[sessionId]

    /**
     * Get all active sessions
     */
    fun getActiveSessions(): List<Session> {
        return sessions.values.filter { it.state == SessionState.ACTIVE }
    }

    /**
     * Purge all data
     */
    fun purgeAllData() {
        sessions.clear()
    }
}

/**
 * Session data class
 */
data class Session(
    val id: String,
    val appId: String,
    val metadata: Map<String, Any>,
    val startTime: Instant,
    val endTime: Instant? = null,
    val state: SessionState
)
