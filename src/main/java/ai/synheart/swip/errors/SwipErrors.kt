package ai.synheart.swip.errors

/**
 * Base SWIP exception
 */
open class SWIPException(
    val code: String,
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause)

/**
 * Permission denied exception
 */
class PermissionDeniedException(
    message: String = "Health permissions denied",
    cause: Throwable? = null
) : SWIPException("E_PERMISSION_DENIED", message, cause)

/**
 * Invalid configuration exception
 */
class InvalidConfigurationException(
    message: String = "Invalid configuration",
    cause: Throwable? = null
) : SWIPException("E_INVALID_CONFIG", message, cause)

/**
 * Session not found exception
 */
class SessionNotFoundException(
    message: String = "No active session",
    cause: Throwable? = null
) : SWIPException("E_SESSION_NOT_FOUND", message, cause)

/**
 * Data quality exception
 */
class DataQualityException(
    message: String = "Low quality signal",
    cause: Throwable? = null
) : SWIPException("E_SIGNAL_LOW_QUALITY", message, cause)

/**
 * Initialization exception
 */
class InitializationException(
    message: String = "Failed to initialize SDK",
    cause: Throwable? = null
) : SWIPException("E_INITIALIZATION_FAILED", message, cause)

/**
 * Session exception
 */
class SessionException(
    message: String = "Session error",
    cause: Throwable? = null
) : SWIPException("E_SESSION_ERROR", message, cause)

/**
 * Sensor exception
 */
class SensorException(
    message: String = "Sensor error",
    cause: Throwable? = null
) : SWIPException("E_SENSOR_ERROR", message, cause)

/**
 * Model exception
 */
class ModelException(
    message: String = "ML model error",
    cause: Throwable? = null
) : SWIPException("E_MODEL_ERROR", message, cause)

/**
 * Consent exception
 */
class ConsentException(
    message: String = "Consent required",
    cause: Throwable? = null
) : SWIPException("E_CONSENT_REQUIRED", message, cause)

/**
 * Storage exception
 */
class StorageException(
    message: String = "Storage operation failed",
    cause: Throwable? = null
) : SWIPException("E_STORAGE_ERROR", message, cause)
