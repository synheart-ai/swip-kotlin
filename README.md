# SWIP Android SDK

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Platform](https://img.shields.io/badge/platform-Android-green.svg)](https://developer.android.com)
[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=21)

**Quantify your app's impact on human wellness using real-time biosignals and emotion inference**

## Features

- **🔒 Privacy-First**: All processing happens locally on-device by default
- **📱 Biosignal Collection**: Uses synheart-wear-kotlin to read HR and HRV from Health Connect
- **🧠 Emotion Recognition**: On-device emotion classification from biosignals
- **📊 SWIP Score**: Quantitative wellness impact scoring (0-100)
- **🔐 GDPR Compliant**: User consent management and data purging
- **⚡ Kotlin Coroutines**: Modern async/await API
- **🔄 Flow Streaming**: Real-time score and emotion updates

## Installation

### Gradle

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

- **Android SDK**: API 21+ (Android 5.0)
- **Target SDK**: API 34 (Android 14)
- **Health Connect**: Required for biometric data access
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
lifecycleScope.launch {
    try {
        swipManager.initialize()
        swipManager.requestPermissions()
    } catch (e: Exception) {
        Log.e(TAG, "Failed to initialize SWIP", e)
    }
}
```

### 3. Start a Session

```kotlin
lifecycleScope.launch {
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
}
```

### 4. Stop a Session

```kotlin
lifecycleScope.launch {
    val results = swipManager.stopSession()
    val summary = results.getSummary()

    Log.d(TAG, "Average Score: ${summary["average_swip_score"]}")
    Log.d(TAG, "Dominant Emotion: ${summary["dominant_emotion"]}")
}
```

## API Reference

### SwipSdkManager

#### Methods

- `suspend fun initialize()` - Initialize the SDK
- `suspend fun requestPermissions(): Set<String>` - Request health permissions
- `suspend fun startSession(appId: String, metadata: Map<String, Any>): String` - Start a session
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

```kotlin
data class SwipScoreResult(
    val swipScore: Double,           // 0-100 wellness score
    val dominantEmotion: String,     // "Calm", "Stressed", etc.
    val emotionProbabilities: Map<String, Double>,
    val hrv: Double,                 // HRV SDNN in ms
    val heartRate: Double,           // HR in BPM
    val timestamp: Instant,
    val confidence: Double,
    val dataQuality: Double
)

enum class ConsentLevel {
    ON_DEVICE,       // Local processing only (default)
    LOCAL_EXPORT,    // Manual export allowed
    DASHBOARD_SHARE  // Aggregated data sharing
}
```

## Architecture

```
Health Connect → synheart-wear-kotlin → swip-core-kotlin → swip-kotlin
```

The SDK uses:
- **synheart-wear-kotlin** for biosignal collection from Health Connect
- **swip-core-kotlin** for HRV feature extraction and SWIP score computation
- **Internal emotion engine** for on-device emotion classification

## Privacy

- **Local-first**: All processing happens on-device by default
- **Explicit Consent**: Required before any data sharing
- **GDPR Compliance**: `purgeAllData()` deletes all user data
- **No Raw Biosignals**: Only aggregated metrics transmitted (if consent given)
- **Anonymization**: Hashed device IDs, per-session UUIDs

## Testing

```bash
# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Generate coverage report
./gradlew jacocoTestReport
```

## License

Copyright 2025 Synheart AI

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for details.

## Support

- **Issues**: https://github.com/synheart-ai/swip/issues
- **Docs**: https://swip.synheart.ai/docs
- **Email**: dev@synheart.ai

---

Part of the Synheart Wellness Impact Protocol (SWIP) open standard.
