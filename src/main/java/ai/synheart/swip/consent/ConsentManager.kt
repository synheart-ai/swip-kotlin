package ai.synheart.swip.consent

import ai.synheart.swip.errors.ConsentException
import ai.synheart.swip.models.ConsentContext
import ai.synheart.swip.models.ConsentLevel
import ai.synheart.swip.models.ConsentRecord
import ai.synheart.swip.models.ConsentStatus
import android.content.Context
import android.content.SharedPreferences
import java.time.Duration
import java.time.Instant

/**
 * Consent management for SWIP SDK
 *
 * Implements privacy-first design with explicit consent gates
 * for all data sharing operations.
 */
class ConsentManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private var _currentLevel: ConsentLevel = loadCurrentLevel()
    val currentLevel: ConsentLevel
        get() = _currentLevel

    private val grantHistory = mutableMapOf<ConsentLevel, Instant>()
    private val grantReasons = mutableMapOf<ConsentLevel, String>()

    init {
        loadConsentHistory()
    }

    /**
     * Check if a specific action is allowed
     */
    fun canPerformAction(required: ConsentLevel): Boolean {
        return _currentLevel.allows(required)
    }

    /**
     * Request consent for a specific level
     *
     * This should show UI to the user explaining what data will be shared.
     * Returns true if user grants consent, false otherwise.
     */
    suspend fun requestConsent(
        requested: ConsentLevel,
        context: ConsentContext,
        customMessage: String? = null
    ): Boolean {
        // If we already have sufficient consent, return true
        if (_currentLevel.allows(requested)) {
            return true
        }

        // Show consent dialog (this would be implemented by the app)
        val granted = showConsentDialog(requested, context, customMessage)

        if (granted) {
            grantConsent(requested, context.reason)
        }

        return granted
    }

    /**
     * Grant consent for a specific level
     */
    suspend fun grantConsent(level: ConsentLevel, reason: String) {
        _currentLevel = level
        grantHistory[level] = Instant.now()
        grantReasons[level] = reason

        // Persist consent
        persistConsent()
    }

    /**
     * Revoke consent (downgrade to onDevice)
     */
    suspend fun revokeConsent() {
        _currentLevel = ConsentLevel.ON_DEVICE
        persistConsent()
    }

    /**
     * Purge all user data (GDPR compliance)
     */
    suspend fun purgeAllData() {
        // This would trigger deletion of all stored data
        purgeAllStoredData()

        // Reset consent
        _currentLevel = ConsentLevel.ON_DEVICE
        grantHistory.clear()
        grantReasons.clear()
        persistConsent()
    }

    /**
     * Get consent history for audit trail
     */
    fun getConsentHistory(): Map<ConsentLevel, ConsentRecord> {
        val history = mutableMapOf<ConsentLevel, ConsentRecord>()

        for (level in ConsentLevel.values()) {
            if (grantHistory.containsKey(level)) {
                history[level] = ConsentRecord(
                    level = level,
                    grantedAt = grantHistory[level]!!,
                    reason = grantReasons[level] ?: "Unknown"
                )
            }
        }

        return history
    }

    /**
     * Check if consent is still valid (not expired)
     */
    fun isConsentValid(level: ConsentLevel): Boolean {
        val grantedAt = grantHistory[level] ?: return false
        val now = Instant.now()

        // Consent expires after 1 year
        val expirationDate = grantedAt.plus(Duration.ofDays(365))
        return now.isBefore(expirationDate)
    }

    /**
     * Get consent status for all levels
     */
    fun getConsentStatus(): Map<ConsentLevel, ConsentStatus> {
        val status = mutableMapOf<ConsentLevel, ConsentStatus>()

        for (level in ConsentLevel.values()) {
            status[level] = if (level.level <= _currentLevel.level) {
                ConsentStatus.GRANTED
            } else {
                ConsentStatus.DENIED
            }
        }

        return status
    }

    // Private methods

    private suspend fun showConsentDialog(
        requested: ConsentLevel,
        context: ConsentContext,
        customMessage: String?
    ): Boolean {
        // This is a placeholder - the actual implementation would show
        // a proper consent dialog to the user
        val message = customMessage ?: getDefaultConsentMessage(requested, context)

        // For now, return false (deny consent)
        // In a real implementation, this would show UI and return user's choice
        println("Consent requested: $message")
        return false
    }

    private fun getDefaultConsentMessage(requested: ConsentLevel, context: ConsentContext): String {
        return when (requested) {
            ConsentLevel.ON_DEVICE ->
                "SWIP will process your data locally on your device. No data will be shared."
            ConsentLevel.LOCAL_EXPORT ->
                "You can export your SWIP data locally. No automatic sharing will occur."
            ConsentLevel.DASHBOARD_SHARE ->
                "Aggregated SWIP data may be shared with the SWIP Dashboard for research. Raw biosignals will never be transmitted."
        }
    }

    private suspend fun persistConsent() {
        prefs.edit().apply {
            putInt(KEY_CURRENT_LEVEL, _currentLevel.level)

            // Save grant history
            for ((level, instant) in grantHistory) {
                putLong("${KEY_GRANT_TIME}_${level.level}", instant.toEpochMilli())
                putString("${KEY_GRANT_REASON}_${level.level}", grantReasons[level])
            }

            apply()
        }

        println("Consent persisted: $_currentLevel")
    }

    private fun loadCurrentLevel(): ConsentLevel {
        val levelValue = prefs.getInt(KEY_CURRENT_LEVEL, ConsentLevel.ON_DEVICE.level)
        return ConsentLevel.values().first { it.level == levelValue }
    }

    private fun loadConsentHistory() {
        for (level in ConsentLevel.values()) {
            val grantTime = prefs.getLong("${KEY_GRANT_TIME}_${level.level}", -1)
            if (grantTime != -1L) {
                grantHistory[level] = Instant.ofEpochMilli(grantTime)
                grantReasons[level] = prefs.getString("${KEY_GRANT_REASON}_${level.level}", "Unknown") ?: "Unknown"
            }
        }
    }

    private suspend fun purgeAllStoredData() {
        prefs.edit().clear().apply()
        println("All user data purged")
    }

    companion object {
        private const val PREFS_NAME = "swip_consent"
        private const val KEY_CURRENT_LEVEL = "current_level"
        private const val KEY_GRANT_TIME = "grant_time"
        private const val KEY_GRANT_REASON = "grant_reason"
    }
}

/**
 * Consent validation utilities
 */
object ConsentValidator {
    /**
     * Validate that required consent is present for an operation
     */
    fun validateConsent(
        required: ConsentLevel,
        current: ConsentLevel,
        operation: String? = null
    ) {
        if (!current.allows(required)) {
            throw ConsentException(
                "Operation \"${operation ?: "unknown"}\" requires consent level $required, " +
                        "but current level is $current"
            )
        }
    }

    /**
     * Check if consent is valid for a specific operation
     */
    fun isValidForOperation(
        required: ConsentLevel,
        current: ConsentLevel
    ): Boolean {
        return current.allows(required)
    }
}
