package ai.synheart.swip.example

import ai.synheart.swip.SwipSdkManager
import ai.synheart.swip.models.*
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var swipManager: SwipSdkManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize SWIP SDK
        swipManager = SwipSdkManager(
            context = this,
            config = SwipSdkConfig(enableLogging = true)
        )

        // Initialize SDK
        lifecycleScope.launch {
            try {
                swipManager.initialize()
                swipManager.requestPermissions()
            } catch (e: Exception) {
                println("Failed to initialize SWIP: ${e.message}")
            }
        }

        setContent {
            SwipExampleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SwipExampleApp(swipManager)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipExampleApp(swipManager: SwipSdkManager) {
    var sessionActive by remember { mutableStateOf(false) }
    var currentScore by remember { mutableStateOf<SwipScoreResult?>(null) }
    var sessionId by remember { mutableStateOf<String?>(null) }
    var sessionResults by remember { mutableStateOf<SwipSessionResults?>(null) }

    val coroutineScope = rememberCoroutineScope()

    // Collect scores
    LaunchedEffect(sessionActive) {
        if (sessionActive) {
            swipManager.scoreFlow.collect { score ->
                currentScore = score
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SWIP SDK Example") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Session Status
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (sessionActive)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Session Status",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (sessionActive) "Active" else "Idle",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (sessionId != null) {
                            Text(
                                text = "ID: ${sessionId?.take(8)}...",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Icon(
                        imageVector = if (sessionActive) Icons.Filled.PlayArrow else Icons.Filled.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            // Current SWIP Score
            if (currentScore != null) {
                ScoreCard(currentScore!!)
            } else if (!sessionActive) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Start a session to see SWIP scores",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Session Control Buttons
            if (!sessionActive) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                val id = swipManager.startSession(
                                    appId = "ai.synheart.swip.example",
                                    metadata = mapOf("screen" to "main")
                                )
                                sessionId = id
                                sessionActive = true
                                sessionResults = null
                            } catch (e: Exception) {
                                println("Failed to start session: ${e.message}")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Start Session")
                }
            } else {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                val results = swipManager.stopSession()
                                sessionResults = results
                                sessionActive = false
                                currentScore = null
                            } catch (e: Exception) {
                                println("Failed to stop session: ${e.message}")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Stop Session")
                }
            }

            // Session Results
            if (sessionResults != null) {
                SessionResultsCard(sessionResults!!)
            }
        }
    }
}

@Composable
fun ScoreCard(score: SwipScoreResult) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Current SWIP Score",
                style = MaterialTheme.typography.titleMedium
            )

            // Score Display
            Text(
                text = String.format("%.1f", score.swipScore),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = getScoreColor(score.swipScore)
            )

            // Emotion
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = getEmotionIcon(score.dominantEmotion),
                    contentDescription = null
                )
                Column {
                    Text(
                        text = "Emotion: ${score.dominantEmotion}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Confidence: ${String.format("%.1f%%", score.confidence * 100)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Divider()

            // Vitals
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                VitalChip("HR", String.format("%.0f", score.heartRate), "BPM")
                VitalChip("HRV", String.format("%.0f", score.hrv), "ms")
            }
        }
    }
}

@Composable
fun SessionResultsCard(results: SwipSessionResults) {
    val summary = results.getSummary()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Session Complete!",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text("Duration: ${summary["duration_seconds"]} seconds")
            Text("Average Score: ${String.format("%.1f", summary["average_swip_score"])}")
            Text("Dominant Emotion: ${summary["dominant_emotion"]}")
            Text("Total Scores: ${summary["score_count"]}")
        }
    }
}

@Composable
fun VitalChip(label: String, value: String, unit: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = unit,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun getScoreColor(score: Double): androidx.compose.ui.graphics.Color {
    return when {
        score >= 80 -> MaterialTheme.colorScheme.primary
        score >= 60 -> MaterialTheme.colorScheme.tertiary
        score >= 40 -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.error
    }
}

fun getEmotionIcon(emotion: String) = when (emotion.lowercase()) {
    "amused" -> Icons.Filled.SentimentSatisfied
    "calm" -> Icons.Filled.Favorite
    "stressed" -> Icons.Filled.Warning
    else -> Icons.Filled.Info
}

@Composable
fun SwipExampleTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(),
        content = content
    )
}
