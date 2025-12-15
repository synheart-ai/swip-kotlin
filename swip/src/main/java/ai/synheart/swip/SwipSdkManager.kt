package ai.synheart.swip

import ai.synheart.swip.consent.ConsentManager
import ai.synheart.swip.errors.*
import ai.synheart.swip.ml.EmotionEngine
import ai.synheart.swip.ml.SwipEngine
import ai.synheart.swip.models.*
import ai.synheart.swip.session.SessionManager
import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.Instant
import java.util.UUID

/**
 * SWIP SDK Manager - Main entry point for the Android SDK
 *
 * Integrates:
 * - Health Connect: Reads HR, HRV data
 * - EmotionEngine: Runs emotion inference models
 * - SwipEngine: Computes SWIP Score
 *
 * @property context Application context
 * @property config SDK configuration
 */
class SwipSdkManager(
    private val context: Context,
    private val config: SwipSdkConfig = SwipSdkConfig()
) {
    // Core components
    private var healthConnectClient: HealthConnectClient? = null
    private val emotionEngine: EmotionEngine = EmotionEngine(config.emotionConfig, context)
    private val swipEngine: SwipEngine = SwipEngine(config.swipConfig)
    private val consentManager: ConsentManager = ConsentManager(context)
    private val sessionManager: SessionManager = SessionManager()

    // State
    private var initialized = false
    private var activeSessionId: String? = null

    // Coroutine scope
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Streams
    private val _scoreFlow = MutableSharedFlow<SwipScoreResult>(replay = 0)
    val scoreFlow: SharedFlow<SwipScoreResult> = _scoreFlow.asSharedFlow()

    private val _emotionFlow = MutableSharedFlow<EmotionResult>(replay = 0)
    val emotionFlow: SharedFlow<EmotionResult> = _emotionFlow.asSharedFlow()

    // Session data
    private val sessionScores = mutableListOf<SwipScoreResult>()
    private val sessionEmotions = mutableListOf<EmotionResult>()

    // Processing job
    private var processingJob: Job? = null

    /**
     * Initialize the SDK
     *
     * @throws InitializationException if initialization fails
     */
    suspend fun initialize() {
        if (initialized) return

        try {
            // Check if Health Connect is available
            val availability = HealthConnectClient.getSdkStatus(context)
            if (availability != HealthConnectClient.SDK_AVAILABLE) {
                throw InitializationException("Health Connect not available")
            }

            // Initialize Health Connect client
            healthConnectClient = HealthConnectClient.getOrCreate(context)

            initialized = true
            log("info", "SWIP SDK initialized")
        } catch (e: Exception) {
            log("error", "Failed to initialize: ${e.message}")
            throw InitializationException("Failed to initialize SWIP SDK", e)
        }
    }

    /**
     * Request health permissions
     *
     * @return Set of granted permissions
     */
    suspend fun requestPermissions(): Set<String> {
        if (!initialized) {
            throw InvalidConfigurationException("SWIP SDK not initialized")
        }

        val client = healthConnectClient ?: throw InitializationException("Health Connect client not initialized")

        val permissions = setOf(
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class)
        )

        return client.permissionController.getGrantedPermissions()
    }

    /**
     * Start a session for an app
     *
     * @param appId Application identifier
     * @param metadata Optional session metadata
     * @return Session ID
     * @throws SessionException if session cannot be started
     */
    suspend fun startSession(
        appId: String,
        metadata: Map<String, Any> = emptyMap()
    ): String {
        if (!initialized) {
            throw InvalidConfigurationException("SWIP SDK not initialized")
        }

        if (activeSessionId != null) {
            throw SessionException("Session already in progress")
        }

        // Generate session ID
        activeSessionId = "${Instant.now().toEpochMilli()}_$appId"

        try {
            // Clear previous session data
            sessionScores.clear()
            sessionEmotions.clear()

            // Start session in session manager
            sessionManager.startSession(activeSessionId!!, appId, metadata)

            // Start data processing
            startDataProcessing()

            log("info", "Session started: $activeSessionId")
            return activeSessionId!!
        } catch (e: Exception) {
            activeSessionId = null
            log("error", "Failed to start session: ${e.message}")
            throw SessionException("Failed to start session", e)
        }
    }

    /**
     * Stop the current session
     *
     * @return Session results
     * @throws SessionException if no active session
     */
    suspend fun stopSession(): SwipSessionResults {
        val sessionId = activeSessionId ?: throw SessionException("No active session")

        try {
            // Stop data processing
            stopDataProcessing()

            // Create session results
            val session = sessionManager.getSession(sessionId)
                ?: throw SessionException("Session not found")

            val results = SwipSessionResults(
                sessionId = sessionId,
                scores = sessionScores.toList(),
                emotions = sessionEmotions.toList(),
                startTime = session.startTime,
                endTime = Instant.now()
            )

            // End session in session manager
            sessionManager.endSession(sessionId)

            // Clear session data
            sessionScores.clear()
            sessionEmotions.clear()
            activeSessionId = null

            log("info", "Session stopped: $sessionId")
            return results
        } catch (e: Exception) {
            log("error", "Failed to stop session: ${e.message}")
            throw SessionException("Failed to stop session", e)
        }
    }

    /**
     * Get current SWIP score
     */
    fun getCurrentScore(): SwipScoreResult? = sessionScores.lastOrNull()

    /**
     * Get current emotion
     */
    fun getCurrentEmotion(): EmotionResult? = sessionEmotions.lastOrNull()

    /**
     * Set user consent level
     */
    suspend fun setUserConsent(level: ConsentLevel, reason: String) {
        consentManager.grantConsent(level, reason)
    }

    /**
     * Get current consent level
     */
    fun getUserConsent(): ConsentLevel = consentManager.currentLevel

    /**
     * Purge all user data (GDPR compliance)
     */
    suspend fun purgeAllData() {
        // Stop any active session
        if (activeSessionId != null) {
            try {
                stopSession()
            } catch (e: Exception) {
                log("warn", "Failed to stop session during purge: ${e.message}")
            }
        }

        // Clear all data
        sessionScores.clear()
        sessionEmotions.clear()
        sessionManager.purgeAllData()
        consentManager.purgeAllData()

        log("info", "All user data purged")
    }

    /**
     * Dispose resources
     */
    fun dispose() {
        processingJob?.cancel()
        scope.cancel()
        log("info", "SWIP SDK disposed")
    }

    // Private methods

    private fun startDataProcessing() {
        processingJob = scope.launch {
            while (isActive) {
                try {
                    processHealthData()
                    delay(1000) // Process every second
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    log("warn", "Error processing health data: ${e.message}")
                }
            }
        }
    }

    private fun stopDataProcessing() {
        processingJob?.cancel()
        processingJob = null
    }

    private suspend fun processHealthData() {
        val client = healthConnectClient ?: return

        try {
            // Read heart rate data from Health Connect
            val now = Instant.now()
            val fiveSecondsAgo = now.minusSeconds(5)

            // This is a simplified version - real implementation would query Health Connect
            // and process actual data
            val hr = readLatestHeartRate(client, fiveSecondsAgo, now)
            val hrv = readLatestHRV(client, fiveSecondsAgo, now)

            if (hr != null && hrv != null) {
                // Push to emotion engine
                emotionEngine.push(
                    hr = hr,
                    hrv = hrv,
                    timestamp = now,
                    motion = 0.0
                )

                // Get emotion result if ready
                val emotionResults = emotionEngine.consumeReady()
                if (emotionResults.isNotEmpty()) {
                    val latestEmotion = emotionResults.last()
                    sessionEmotions.add(latestEmotion)
                    _emotionFlow.emit(latestEmotion)

                    // Compute SWIP score
                    val swipResult = swipEngine.computeScore(
                        hr = hr,
                        hrv = hrv,
                        motion = 0.0,
                        emotionProbabilities = latestEmotion.probabilities
                    )

                    sessionScores.add(swipResult)
                    _scoreFlow.emit(swipResult)

                    log("debug", "SWIP Score: ${String.format("%.1f", swipResult.swipScore)}")
                }
            }
        } catch (e: Exception) {
            log("warn", "Failed to process health data: ${e.message}")
        }
    }

    private suspend fun readLatestHeartRate(
        client: HealthConnectClient,
        startTime: Instant,
        endTime: Instant
    ): Double? {
        // Simplified - real implementation would query Health Connect
        // For now, return a mock value for demonstration
        return 75.0
    }

    private suspend fun readLatestHRV(
        client: HealthConnectClient,
        startTime: Instant,
        endTime: Instant
    ): Double? {
        // Simplified - real implementation would query Health Connect
        // For now, return a mock value for demonstration
        return 50.0
    }

    private fun log(level: String, message: String) {
        if (config.enableLogging) {
            println("[SWIP SDK] [$level] $message")
        }
    }
}
