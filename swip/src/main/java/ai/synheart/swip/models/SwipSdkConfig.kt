package ai.synheart.swip.models

/**
 * Configuration for SWIP SDK
 */
data class SwipSdkConfig(
    val swipConfig: SwipConfig = SwipConfig(),
    val emotionConfig: EmotionConfig = EmotionConfig(),
    val enableLogging: Boolean = true,
    val enableLocalStorage: Boolean = true,
    val localStoragePath: String? = null
)

/**
 * Configuration for SWIP Score computation
 */
data class SwipConfig(
    val weightHrv: Double = 0.5,
    val weightCoherence: Double = 0.3,
    val weightRecovery: Double = 0.2,
    val beneficialThreshold: Double = 0.2,
    val harmfulThreshold: Double = -0.2
)

/**
 * Configuration for Emotion Recognition
 */
data class EmotionConfig(
    val modelPath: String? = null,
    val useOnDeviceModel: Boolean = true,
    val confidenceThreshold: Double = 0.6
) {
    companion object {
        val defaultConfig = EmotionConfig()
    }
}

/**
 * Session configuration
 */
data class SWIPSessionConfig(
    val appId: String,
    val metadata: Map<String, Any> = emptyMap()
)
