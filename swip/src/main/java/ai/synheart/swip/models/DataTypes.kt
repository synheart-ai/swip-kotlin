package ai.synheart.swip.models

/**
 * Data types and enums for SWIP SDK
 */

/**
 * Consent levels for data sharing
 */
enum class ConsentLevel(val level: Int) {
    /** Level 0: On-device only (default) */
    ON_DEVICE(0),

    /** Level 1: Local export allowed */
    LOCAL_EXPORT(1),

    /** Level 2: Dashboard sharing allowed */
    DASHBOARD_SHARE(2);

    /**
     * Check if this level allows the requested action
     */
    fun allows(required: ConsentLevel): Boolean {
        return level >= required.level
    }

    /**
     * Get human-readable description
     */
    fun getDescription(): String {
        return when (this) {
            ON_DEVICE -> "On-device only - no data sharing"
            LOCAL_EXPORT -> "Local export - manual data export allowed"
            DASHBOARD_SHARE -> "Dashboard sharing - aggregated data can be uploaded"
        }
    }
}

/**
 * SWIP Score interpretation ranges
 */
enum class SwipScoreRange(val min: Int, val max: Int) {
    POSITIVE(80, 100),
    NEUTRAL(60, 79),
    MILD_STRESS(40, 59),
    NEGATIVE(0, 39);

    /**
     * Check if score falls in this range
     */
    fun contains(score: Double): Boolean {
        return score >= min && score <= max
    }

    /**
     * Get human-readable description
     */
    fun getDescription(): String {
        return when (this) {
            POSITIVE -> "Relaxed/Engaged - app supports wellness"
            NEUTRAL -> "Emotionally stable"
            MILD_STRESS -> "Cognitive or emotional fatigue"
            NEGATIVE -> "Stress/emotional load detected"
        }
    }

    companion object {
        fun forScore(score: Double): SwipScoreRange {
            return values().firstOrNull { it.contains(score) } ?: NEGATIVE
        }
    }
}

/**
 * Emotion classes supported by the system
 */
enum class EmotionClass(val label: String, val utility: Double) {
    AMUSED("Amused", 0.95),
    CALM("Calm", 0.85),
    FOCUSED("Focused", 0.80),
    NEUTRAL("Neutral", 0.70),
    STRESSED("Stressed", 0.15);

    companion object {
        fun fromLabel(label: String): EmotionClass? {
            return values().firstOrNull { it.label.equals(label, ignoreCase = true) }
        }
    }
}

/**
 * Session states
 */
enum class SessionState {
    IDLE,
    STARTING,
    ACTIVE,
    STOPPING,
    ENDED,
    ERROR;

    val isActive: Boolean
        get() = this == ACTIVE

    val canStart: Boolean
        get() = this == IDLE || this == ENDED

    val canStop: Boolean
        get() = this == ACTIVE || this == STARTING
}

/**
 * Data quality levels
 */
enum class DataQuality(val minScore: Double, val maxScore: Double) {
    HIGH(0.7, 1.0),
    MEDIUM(0.4, 0.7),
    LOW(0.0, 0.4);

    val isAcceptable: Boolean
        get() = this != LOW

    companion object {
        fun forScore(score: Double): DataQuality {
            return values().firstOrNull { score >= it.minScore && score < it.maxScore } ?: LOW
        }
    }
}

/**
 * Consent status
 */
enum class ConsentStatus {
    GRANTED,
    DENIED,
    EXPIRED
}

/**
 * Consent context for requests
 */
data class ConsentContext(
    val appId: String,
    val reason: String,
    val metadata: Map<String, Any>? = null
)

/**
 * Consent record for audit trail
 */
data class ConsentRecord(
    val level: ConsentLevel,
    val grantedAt: java.time.Instant,
    val reason: String
) {
    fun toJson(): Map<String, Any> {
        return mapOf(
            "level" to level.level,
            "level_name" to level.name,
            "granted_at" to grantedAt.toString(),
            "reason" to reason
        )
    }

    companion object {
        fun fromJson(json: Map<String, Any>): ConsentRecord {
            return ConsentRecord(
                level = ConsentLevel.values().first { it.level == json["level"] as Int },
                grantedAt = java.time.Instant.parse(json["granted_at"] as String),
                reason = json["reason"] as String
            )
        }
    }
}
