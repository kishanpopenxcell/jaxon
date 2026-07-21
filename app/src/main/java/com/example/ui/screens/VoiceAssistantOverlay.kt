package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.speech.SpeechState
import com.example.ui.theme.GlowCyan
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.DeepGray
import com.example.ui.theme.SpaceBlack
import com.example.ui.theme.SoftTextGray
import com.example.ui.theme.WarningRed
import androidx.compose.foundation.BorderStroke
import java.util.Locale
import com.example.ui.viewmodel.JaxonViewModel
import kotlin.math.sin

@Composable
fun VoiceAssistantOverlay(
    viewModel: JaxonViewModel,
    modifier: Modifier = Modifier
) {
    val showOverlay by viewModel.showListeningOverlay.collectAsState()
    if (!showOverlay) return

    val speechState by viewModel.speechState.collectAsState()
    val partialText by viewModel.partialText.collectAsState()
    val activeResult by viewModel.activeResultText.collectAsState()
    val rmsDb by viewModel.rmsDb.collectAsState()
    val errorText by viewModel.speechError.collectAsState()
    val isExecuting by viewModel.isExecutingAction.collectAsState()

    // Breathing pulse for the glowing AI core
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.12f,
        animationSpec = infiniteSpec(1500),
        label = "pulse_scale"
    )
    val coreGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteSpec(1500),
        label = "core_glow"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f))
            .clickable(enabled = false) {} // block click propagation
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Upper background blur effect matching Tailwind purple/blue blur
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(40.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            ElectricPurple.copy(alpha = 0.12f),
                            GlowCyan.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Floating premium card sheet
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(DeepGray.copy(alpha = 0.96f))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(28.dp))
                .padding(horizontal = 24.dp, vertical = 28.dp)
                .testTag("voice_assistant_sheet"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // Header with Pulse Dot & Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Pulsing Status Dot
                    val pulseAnim = rememberInfiniteTransition(label = "dot_pulse")
                    val dotAlpha by pulseAnim.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 1.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dot_alpha"
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(GlowCyan.copy(alpha = dotAlpha), CircleShape)
                    )
                    Text(
                        text = "JAXON ASSISTANT",
                        color = SoftTextGray,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                IconButton(
                    onClick = { viewModel.closeListeningOverlay() },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.White.copy(alpha = 0.06f), CircleShape)
                        .testTag("close_assistant_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Voice Assistant",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Current Status & Large Title Block (Bold Typography Highlight)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Current Status".uppercase(Locale.getDefault()),
                    color = GlowCyan.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )

                val textGradient = Brush.linearGradient(
                    colors = listOf(Color.White, Color(0xFF64748B))
                )
                val statusTitle = when {
                    isExecuting -> "Processing..."
                    speechState == SpeechState.READY -> "Ready..."
                    speechState == SpeechState.LISTENING -> "Listening..."
                    speechState == SpeechState.PROCESSING -> "Analyzing..."
                    speechState == SpeechState.SUCCESS -> "Finished"
                    speechState == SpeechState.ERROR -> "Error!"
                    else -> "Waiting..."
                }
                Text(
                    text = statusTitle,
                    style = TextStyle(
                        brush = textGradient,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1.5).sp,
                        lineHeight = 42.sp
                    ),
                    textAlign = TextAlign.Center
                )
            }

            // Glowing AI orb or Waveform
            Box(
                modifier = Modifier
                    .size(130.dp),
                contentAlignment = Alignment.Center
            ) {
                if (speechState == SpeechState.LISTENING || speechState == SpeechState.READY) {
                    // Render Concentric Pulsating Rings
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(10.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        GlowCyan.copy(alpha = coreGlowAlpha),
                                        ElectricPurple.copy(alpha = 0.08f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Waveform Canvas Visualizer (No external library, custom drawn with physical DB input!)
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    ) {
                        val numBars = 10
                        val spacing = 6.dp.toPx()
                        val barWidth = 6.dp.toPx()
                        val containerWidth = size.width
                        val containerHeight = size.height
                        val midY = containerHeight / 2f

                        // Calculate total width of all bars and spacing
                        val totalWidth = (numBars * barWidth) + ((numBars - 1) * spacing)
                        val startX = (containerWidth - totalWidth) / 2f

                        // Map rmsDb to a scale height multiplier
                        val baseHeightMultiplier = 16.dp.toPx()
                        val micHeightMultiplier = rmsDb * 28.dp.toPx()
                        val totalHeightScale = baseHeightMultiplier + micHeightMultiplier

                        for (i in 0 until numBars) {
                            // Phase shift to create a beautiful sine propagation across bars
                            val phase = (i.toFloat() / numBars) * Math.PI * 2.0
                            val timeFactor = (System.currentTimeMillis() % 1000) / 1000f * Math.PI * 2.0
                            val waveVal = sin(phase + timeFactor).toFloat()

                            // Height of the bar is modulated by wave value and mic volume
                            val height = (0.2f + 0.8f * waveVal.coerceIn(0f, 1f)) * totalHeightScale
                            val barX = startX + i * (barWidth + spacing)

                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(GlowCyan, ElectricPurple)
                                ),
                                topLeft = Offset(barX, midY - (height / 2f)),
                                size = Size(barWidth, height),
                                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
                            )
                        }
                    }
                } else {
                    // Pulsating Glowing Core (Processing, Idle or Result states)
                    Box(
                        modifier = Modifier
                            .size(80.dp * pulseScale)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(GlowCyan, ElectricPurple)
                                )
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isExecuting) Icons.Default.VolumeUp else Icons.Default.Mic,
                            contentDescription = "Active Mic",
                            tint = Color.Black,
                            modifier = Modifier
                                .size(28.dp)
                                .align(Alignment.Center)
                        )
                    }
                }
            }

            // Real-time voice transcript in bold style
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp, max = 90.dp)
                    .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(18.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(18.dp))
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (partialText.isNotBlank()) "\"$partialText\"" else "Speak your request...",
                    color = if (partialText.isNotBlank()) Color.White else SoftTextGray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            // Results, Errors, or Actions panel
            AnimatedVisibility(
                visible = activeResult != null || errorText != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (errorText != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = WarningRed.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.border(1.dp, WarningRed.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
                        ) {
                            Text(
                                text = errorText ?: "",
                                color = WarningRed,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(14.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    if (activeResult != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.border(1.dp, GlowCyan.copy(alpha = 0.1f), RoundedCornerShape(18.dp))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "JAXON RESPONSE",
                                    color = GlowCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp,
                                    fontFamily = FontFamily.Monospace
                                )

                                Text(
                                    text = activeResult ?: "",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }

            // Control Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (speechState == SpeechState.LISTENING || speechState == SpeechState.READY) {
                    Button(
                        onClick = { viewModel.stopListening() },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("stop_listening_button")
                    ) {
                        Text("Finish Speaking", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                } else {
                    Button(
                        onClick = { viewModel.startListening() },
                        colors = ButtonDefaults.buttonColors(containerColor = GlowCyan),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(48.dp)
                            .testTag("retry_button")
                    ) {
                        Icon(Icons.Default.MicNone, contentDescription = null, tint = Color.Black)
                        Spacer(Modifier.width(8.dp))
                        Text("Ask Another", fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }

                OutlinedButton(
                    onClick = { viewModel.cancelListening() },
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("cancel_button")
                ) {
                    Text("Dismiss", color = SoftTextGray, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}


@Composable
private fun infiniteSpec(duration: Int): InfiniteRepeatableSpec<Float> {
    return infiniteRepeatable(
        animation = tween(duration, easing = LinearEasing),
        repeatMode = RepeatMode.Reverse
    )
}
