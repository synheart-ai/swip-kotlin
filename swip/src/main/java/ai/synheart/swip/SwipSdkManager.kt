package ai.synheart.swip

import ai.synheart.swip.consent.ConsentManager
import ai.synheart.swip.errors.*
import ai.synheart.swip.ml.EmotionEngine
import ai.synheart.swip.models.*
import ai.synheart.swip.session.SessionManager
import android.content.Context
import ai.synheart.wear.SynheartWear
import ai.synheart.wear.config.SynheartWearConfig
import ai.synheart.wear.models.DeviceAdapter
import ai.synheart.wear.models.PermissionType
import ai.synheart.swip.core.SwipEngine
import ai.synheart.swip.core.PhysiologicalBaseline
import ai.synheart.swip.core.EmotionSnapshot
import ai.synheart.swip.core.SwipConfig as CoreSwipConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.Instant
import java.util.UUID

/**
 * SWIP SDK Manager - Main entry point for the Android SDK
 *
 * Integrates:
 * - SynheartWear: Reads HR, HRV data from Health Connect via biosignal collection layer
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
    private val synheartWear: SynheartWear = SynheartWear(
        context = context,
        config = SynheartWearConfig(
            enabledAdapters = setOf(DeviceAdapter.HEALTH_CONNECT),
            enableLocalCaching = false,
            enableEncryption = true,
            streamInterval = 1000L // 1 second
        )
    )
    private val emotionEngine: EmotionEngine = EmotionEngine(config.emotionConfig, context)
    private val baseline: PhysiologicalBaseline = PhysiologicalBaseline(
        restingHr = 70.0,
        restingHrv = 50.0
    )
    private val swipEngine: SwipEngine = SwipEngine(
        baseline = baseline,
        config = CoreSwipConfig.Default
    )
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
            // Initialize SynheartWear SDK
            synheartWear.initialize()

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

        try {
            val permissions = setOf(
                PermissionType.HEART_RATE,
                PermissionType.HEART_RATE_VARIABILITY
            )

            val granted = synheartWear.requestPermissions(permissions)

            return granted
                .filter { it.value }
                .keys
                .map { it.name }
                .toSet()
        } catch (e: Exception) {
            log("error", "Failed to request permissions: ${e.message}")
            throw InitializationException("Failed to request permissions", e)
        }
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
        try {
            // Read biometric data from SynheartWear
            val metrics = synheartWear.readMetrics(isRealTime = true)

            val hr = metrics.get("hr")
            val hrv = metrics.get("hrv_sdnn")
            val motion = metrics.get("motion") ?: 0.0

            if (hr != null && hrv != null) {
                val now = Instant.now()

                // Push to emotion engine
                emotionEngine.push(
                    hr = hr,
                    hrv = hrv,
                    timestamp = now,
                    motion = motion
                )

                // Get emotion result if ready
                val emotionResults = emotionEngine.consumeReady()
                if (emotionResults.isNotEmpty()) {
                    val latestEmotion = emotionResults.last()
                    sessionEmotions.add(latestEmotion)
                    _emotionFlow.emit(latestEmotion)

                    // Convert EmotionResult to EmotionSnapshot
                    val arousalScore = when (latestEmotion.emotion.lowercase()) {
                        "stressed" -> 0.8
                        "amused", "excited" -> 0.6
                        "calm", "relaxed" -> 0.2
                        else -> 0.5
                    }

                    val emotionSnapshot = EmotionSnapshot(
                        arousalScore = arousalScore,
                        state = latestEmotion.emotion,
                        confidence = latestEmotion.confidence,
                        warmingUp = false
                    )

                    // Compute SWIP score
                    val swipResult = swipEngine.computeScore(
                        hr = hr,
                        hrv = hrv,
                        motion = motion,
                        emotion = emotionSnapshot
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

    private fun log(level: String, message: String) {
        if (config.enableLogging) {
            println("[SWIP SDK] [$level] $message")
        }
    }
}
