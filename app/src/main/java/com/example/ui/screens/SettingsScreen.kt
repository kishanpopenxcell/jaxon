package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.ui.theme.GlowCyan
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.DeepGray
import com.example.ui.theme.SpaceBlack
import com.example.ui.theme.WarningRed
import com.example.ui.theme.LightGlassGray
import com.example.ui.theme.GlassBorder
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Brush
import com.example.ui.viewmodel.JaxonViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: JaxonViewModel,
    modifier: Modifier = Modifier
) {
    val isTtsActive by viewModel.isTtsEnabled.collectAsState()
    val ttsRateVal by viewModel.ttsRate.collectAsState()
    val ttsPitchVal by viewModel.ttsPitch.collectAsState()
    val isBgActive by viewModel.isBackgroundServiceEnabled.collectAsState()
    val activeLocale by viewModel.recognitionLocale.collectAsState()

    var showClearConfirm by remember { mutableStateOf(false) }
    var showLanguagePicker by remember { mutableStateOf(false) }

    val languages = mapOf(
        "en-US" to "English (United States)",
        "en-GB" to "English (United Kingdom)",
        "es-ES" to "Spanish (Spain)",
        "fr-FR" to "French (France)",
        "de-DE" to "German (Germany)"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SpaceBlack)
            .padding(horizontal = 20.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 40.dp)
    ) {
        // Header
        item {
            Column {
                Text(
                    text = "SETTINGS CENTER",
                    color = GlowCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.height(4.dp))
                
                val textGradient = Brush.linearGradient(
                    colors = listOf(Color.White, Color(0xFF94A3B8))
                )
                Text(
                    text = "System Options",
                    style = TextStyle(
                        brush = textGradient,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1).sp,
                        lineHeight = 34.sp
                    )
                )
            }
        }

        // Voice Engine options
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = LightGlassGray),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, GlassBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "SPEECH ENGINE",
                        color = GlowCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    // TTS Toggle Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = SoftTextGray)
                            Column {
                                Text("Speech Responses", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Acoustic command feedback", color = SoftTextGray, fontSize = 11.sp)
                            }
                        }

                        Switch(
                            checked = isTtsActive,
                            onCheckedChange = { viewModel.setTtsEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = GlowCyan, checkedTrackColor = GlowCyan.copy(alpha = 0.3f)),
                            modifier = Modifier.testTag("tts_switch")
                        )
                    }

                    if (isTtsActive) {
                        // Rate Slider
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Speech Speed Rate", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text(String.format("%.1fx", ttsRateVal), color = GlowCyan, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            }
                            Slider(
                                value = ttsRateVal,
                                onValueChange = { viewModel.setTtsRate(it) },
                                valueRange = 0.5f..2.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = GlowCyan,
                                    activeTrackColor = GlowCyan,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                                ),
                                modifier = Modifier.testTag("tts_rate_slider")
                            )
                        }

                        // Pitch Slider
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Speech Voice Pitch", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text(String.format("%.1fx", ttsPitchVal), color = GlowCyan, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            }
                            Slider(
                                value = ttsPitchVal,
                                onValueChange = { viewModel.setTtsPitch(it) },
                                valueRange = 0.5f..2.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = GlowCyan,
                                    activeTrackColor = GlowCyan,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                                ),
                                modifier = Modifier.testTag("tts_pitch_slider")
                            )
                        }
                    }
                }
            }
        }

        // Language & Locale Selection
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = LightGlassGray),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, GlassBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "VOICE INPUT LOCALE",
                        color = GlowCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showLanguagePicker = !showLanguagePicker }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = SoftTextGray)
                            Column {
                                Text("Assistant Language", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(languages[activeLocale] ?: activeLocale, color = GlowCyan, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }

                        Icon(
                            imageVector = if (showLanguagePicker) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = SoftTextGray
                        )
                    }

                    AnimatedVisibility(
                        visible = showLanguagePicker,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .padding(8.dp)
                        ) {
                            languages.forEach { (code, label) ->
                                val selected = code == activeLocale
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) GlowCyan.copy(alpha = 0.08f) else Color.Transparent)
                                        .clickable {
                                            viewModel.setRecognitionLocale(code)
                                            showLanguagePicker = false
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = label,
                                        color = if (selected) GlowCyan else Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (selected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = GlowCyan, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Navigation Link to Permissions Dashboard
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = LightGlassGray),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, GlassBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate("permissions") }
                    .testTag("permissions_dashboard_row")
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(ElectricPurple.copy(alpha = 0.08f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = ElectricPurple, modifier = Modifier.size(18.dp))
                        }

                        Column {
                            Text("Permissions Center", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Explain and adjust device access rules", color = SoftTextGray, fontSize = 11.sp)
                        }
                    }

                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SoftTextGray)
                }
            }
        }

        // Privacy First Notice
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = LightGlassGray.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, GlassBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.PrivacyTip, contentDescription = null, tint = GlowCyan, modifier = Modifier.size(16.dp))
                        Text(
                            text = "PRIVACY ASSURANCE",
                            color = GlowCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Text(
                        text = "Jaxon's voice cores operate entirely inside your sandbox. Audio recordings are compiled directly to vectors and translated locally using rule pattern dictionaries. Not a single packet or audio fragment is uploaded. Everything runs offline.",
                        color = SoftTextGray,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "VERSION 1.0.0 (OFFLINE)",
                        color = SoftTextGray.copy(alpha = 0.5f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Danger zone - Clear History
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = LightGlassGray.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, WarningRed.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "DANGER ZONE",
                        color = WarningRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Button(
                        onClick = { showClearConfirm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = WarningRed.copy(alpha = 0.12f)),
                        border = BorderStroke(1.dp, WarningRed.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("trigger_clear_history_button")
                    ) {
                        Text("Delete Assistant History", color = WarningRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }

    // Confirm dialogue popup
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Purge Conversations?", color = Color.White) },
            text = { Text("This will permanently clear all recorded voice queries and executions from your local SQLite database. This action cannot be undone.", color = SoftTextGray) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearHistory()
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WarningRed),
                    modifier = Modifier.testTag("confirm_clear_history_button")
                ) {
                    Text("Clear All", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearConfirm = false }
                ) {
                    Text("Cancel", color = SoftTextGray)
                }
            },
            containerColor = LightGlassGray,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

private val SoftTextGray = Color(0xFFA0A0AB)
