# SWIP Android Example App

This example app demonstrates how to integrate and use the SWIP Android SDK in a real Android application.

## Features

- ✅ Session management (start/stop)
- ✅ Real-time SWIP score display
- ✅ Emotion recognition visualization
- ✅ Heart rate and HRV monitoring
- ✅ Session summary and results
- ✅ Material Design 3 UI with Jetpack Compose

## Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- Android SDK 34
- Android device/emulator with Health Connect

## Setup

1. **Install Health Connect** on your device/emulator:
   - Download from Google Play Store
   - Grant necessary permissions

2. **Build and Run**:
   ```bash
   cd sdks/android
   ./gradlew :example:installDebug
   ```

3. **Grant Permissions**:
   - Open the app
   - When prompted, grant Health Connect permissions
   - Start a session

## How It Works

### 1. Initialize SDK

```kotlin
val swipManager = SwipSdkManager(
    context = this,
    config = SwipSdkConfig(enableLogging = true)
)

lifecycleScope.launch {
    swipManager.initialize()
    swipManager.requestPermissions()
}
```

### 2. Start a Session

```kotlin
lifecycleScope.launch {
    val sessionId = swipManager.startSession(
        appId = "ai.synheart.swip.example",
        metadata = mapOf("screen" to "main")
    )
}
```

### 3. Collect Scores

```kotlin
swipManager.scoreFlow.collect { score ->
    println("SWIP Score: ${score.swipScore}")
    println("Emotion: ${score.dominantEmotion}")
}
```

### 4. Stop Session

```kotlin
lifecycleScope.launch {
    val results = swipManager.stopSession()
    val summary = results.getSummary()
    println("Average Score: ${summary["average_swip_score"]}")
}
```

## UI Components

### ScoreCard
Displays current SWIP score with:
- Wellness score (0-100)
- Dominant emotion with icon
- Confidence level
- Heart rate and HRV

### SessionResultsCard
Shows session summary:
- Duration
- Average score
- Dominant emotion
- Total data points

## Testing

To test without real health data:

1. Use the Health Connect TestApp to inject mock data
2. Or modify the SDK to return simulated data
3. The example app will visualize any data stream

## Customization

### Change Theme
Edit `SwipExampleTheme` composable to customize colors and typography.

### Add Features
- Export session data
- Historical data visualization
- Multi-session comparisons
- Consent management UI

## Troubleshooting

**Health Connect not available**:
- Ensure Health Connect is installed
- Check device API level (>= 28)
- Verify app permissions

**No data flowing**:
- Check if Health Connect has data
- Verify permissions are granted
- Enable SDK logging to debug

## License

Apache 2.0 - See LICENSE file
