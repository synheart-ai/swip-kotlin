# SWIP Android SDK

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Platform](https://img.shields.io/badge/platform-Android-green.svg)](https://developer.android.com)
[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=21)

The SWIP Android SDK enables Android applications to quantitatively assess their impact on human wellness using biosignal-based metrics from Health Connect.

## Features

- ✅ **Privacy-first**: All processing happens locally on-device by default
- ✅ **Health Connect Integration**: Reads HR and HRV data from Android Health Connect
- ✅ **Real-time Emotion Recognition**: On-device Linear SVM for emotion classification
- ✅ **SWIP Score Computation**: Quantitative wellness impact scoring
- ✅ **Consent Management**: GDPR-compliant consent and data purging
- ✅ **Kotlin Coroutines**: Modern async/await API
- ✅ **Flow-based Streaming**: Real-time score and emotion updates

## Installation

### Gradle

Add to your `build.gradle`:

```gradle
dependencies {
    implementation 'ai.synheart:swip:1.0.0'
}
```

### Maven

```xml
<dependency>
    <groupId>ai.synheart</groupId>
    <artifactId>swip</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Requirements

- **Android SDK**: API 21 (Android 5.0) or higher
- **Target SDK**: API 34 (Android 14)
- **Health Connect**: Required for health data access
- **Kotlin**: 1.8+

## Quick Start

### 1. Initialize the SDK

```kotlin
import ai.synheart.swip.SwipSdkManager
import ai.synheart.swip.models.SwipSdkConfig

class MyApplication : Application() {
    lateinit var swipManager: SwipSdkManager

    override fun onCreate() {
        super.onCreate()

        swipManager = SwipSdkManager(
            context = this,
            config = SwipSdkConfig(
                enableLogging = true
            )
        )
    }
}
```

### 2. Request Permissions

```kotlin
class MainActivity : ComponentActivity() {
    private lateinit var swipManager: SwipSdkManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            try {
                // Initialize SDK
                swipManager.initialize()

                // Request health permissions
                swipManager.requestPermissions()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize SWIP", e)
            }
        }
    }
}
```

### 3. Start a Session

```kotlin
lifecycleScope.launch {
    try {
        // Start session
        val sessionId = swipManager.startSession(
            appId = "com.example.myapp",
            metadata = mapOf("screen" to "meditation")
        )

        // Collect scores
        swipManager.scoreFlow.collect { score ->
            Log.d(TAG, "SWIP Score: ${score.swipScore}")
            Log.d(TAG, "Emotion: ${score.dominantEmotion}")
        }
    } catch (e: Exception) {
        Log.e(TAG, "Session error", e)
    }
}
```

### 4. Stop a Session

```kotlin
lifecycleScope.launch {
    try {
        val results = swipManager.stopSession()

        val summary = results.getSummary()
        Log.d(TAG, "Session Summary: $summary")
        Log.d(TAG, "Average Score: ${summary["average_swip_score"]}")
        Log.d(TAG, "Dominant Emotion: ${summary["dominant_emotion"]}")
    } catch (e: Exception) {
        Log.e(TAG, "Failed to stop session", e)
    }
}
```

## API Reference

### SwipSdkManager

Main SDK entry point.

#### Methods

- `suspend fun initialize()` - Initialize the SDK
- `suspend fun requestPermissions(): Set<String>` - Request health permissions
- `suspend fun startSession(appId: String, metadata: Map<String, Any> = emptyMap()): String` - Start a session
- `suspend fun stopSession(): SwipSessionResults` - Stop the current session
- `fun getCurrentScore(): SwipScoreResult?` - Get current SWIP score
- `fun getCurrentEmotion(): EmotionResult?` - Get current emotion
- `suspend fun setUserConsent(level: ConsentLevel, reason: String)` - Set consent level
- `fun getUserConsent(): ConsentLevel` - Get current consent level
- `suspend fun purgeAllData()` - Delete all user data (GDPR compliance)

#### Flows

- `scoreFlow: SharedFlow<SwipScoreResult>` - Real-time SWIP scores (~1 Hz)
- `emotionFlow: SharedFlow<EmotionResult>` - Real-time emotion predictions

### Models

#### SwipScoreResult

```kotlin
data class SwipScoreResult(
    val swipScore: Double,           // 0-100 wellness score
    val dominantEmotion: String,     // "Amused", "Calm", "Stressed"
    val emotionProbabilities: Map<String, Double>,
    val hrv: Double,                 // HRV SDNN in ms
    val heartRate: Double,           // HR in BPM
    val timestamp: Instant,
    val confidence: Double,          // Prediction confidence
    val dataQuality: Double          // Signal quality (0-1)
)
```

#### SwipSessionResults

```kotlin
data class SwipSessionResults(
    val sessionId: String,
    val scores: List<SwipScoreResult>,
    val emotions: List<EmotionResult>,
    val startTime: Instant,
    val endTime: Instant
) {
    fun getSummary(): Map<String, Any>
}
```

#### ConsentLevel

```kotlin
enum class ConsentLevel {
    ON_DEVICE,       // Local processing only (default)
    LOCAL_EXPORT,    // Manual export allowed
    DASHBOARD_SHARE  // Aggregated data sharing
}
```

## Privacy & Ethics

The SWIP SDK follows strict privacy requirements:

- **Local-first**: All processing happens on-device by default
- **Explicit Consent**: Required before any data sharing
- **GDPR Compliance**: `purgeAllData()` deletes all user data
- **No Raw Biosignals**: Only aggregated metrics transmitted (if consent given)
- **Anonymization**: Hashed device IDs, per-session UUIDs

## Example App

See `example/` for a complete Android app demonstrating:

- Session management
- Real-time score visualization
- Consent UI implementation
- Data export

## Testing

```bash
# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Generate coverage report
./gradlew jacocoTestReport
```

## Architecture

```
SwipSdkManager
    ├── Health Connect (HR/HRV data)
    ├── EmotionEngine (ML inference)
    │   ├── FeatureExtractor
    │   └── SvmPredictor
    ├── SwipEngine (Score computation)
    ├── ConsentManager (Privacy controls)
    └── SessionManager (Session tracking)
```

## Requirements & Compatibility

- **Minimum SDK**: API 21 (Android 5.0 Lollipop)
- **Target SDK**: API 34 (Android 14)
- **Kotlin**: 1.8.0+
- **Coroutines**: 1.7.0+
- **Health Connect**: 1.1.0+

## Production Readiness

### Current Status

This SDK is in active development. The following items should be addressed before production deployment:

#### ⚠️ Known Issues

1. **Health Connect Integration**: The `readLatestHeartRate()` and `readLatestHRV()` methods in `SwipSdkManager.kt` currently return mock values. These need to be replaced with actual Health Connect API calls to read real health data.

2. **Logging**: The SDK uses `println()` for logging instead of Android's `Log` class. Consider migrating to Android Log or a proper logging framework for better production logging.

3. **Error Handling**: While comprehensive error types exist, ensure all edge cases are properly handled, especially around Health Connect availability and permission states.

4. **Testing**: Ensure comprehensive test coverage, especially for Health Connect integration and edge cases.

#### ✅ Production Ready Features

- ✅ Comprehensive error handling with typed exceptions
- ✅ GDPR-compliant consent management
- ✅ ProGuard rules configured
- ✅ Proper dependency management
- ✅ Kotlin coroutines for async operations
- ✅ Flow-based reactive API
- ✅ Session management
- ✅ On-device ML inference

### Before Production Deployment

1. **Implement Health Connect Integration**: Replace mock data with actual Health Connect queries
2. **Add Comprehensive Tests**: Ensure >80% code coverage
3. **Performance Testing**: Test on various Android devices and API levels
4. **Security Audit**: Review data handling and storage
5. **Documentation**: Complete API documentation
6. **Version Management**: Set up proper versioning strategy

## Contributing

See [CONTRIBUTING.md](../../CONTRIBUTING.md) for guidelines.

## License

Copyright 2024 Synheart AI

Licensed under the Apache License, Version 2.0. See [LICENSE](../../LICENSE) for details.

## Support

- **Issues**: https://github.com/synheart-ai/swip/issues
- **Docs**: https://swip.synheart.ai/docs
- **Email**: dev@synheart.ai

## Acknowledgments

Part of the Synheart Wellness Impact Protocol (SWIP) open standard.

---

**Author**: Israel Goytom

For detailed production readiness information, see [PRODUCTION_READINESS.md](PRODUCTION_READINESS.md).
