package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.data.model.VoiceCommand
import com.example.domain.speech.SpeechState
import com.example.ui.components.JaxonFace
import com.example.ui.theme.GlowCyan
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.DeepGray
import com.example.ui.theme.SpaceBlack
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningRed
import com.example.ui.theme.LightGlassGray
import com.example.ui.theme.GlassBorder
import androidx.compose.ui.text.TextStyle
import com.example.ui.viewmodel.JaxonViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: JaxonViewModel,
    modifier: Modifier = Modifier
) {
    val history by viewModel.historyState.collectAsState()
    val isBgActive by viewModel.isBackgroundServiceEnabled.collectAsState()
    val speechState by viewModel.speechState.collectAsState()

    val context = LocalContext.current
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startListening()
        }
    }

    fun startListeningWithPermissionCheck() {
        val micGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (micGranted) {
            viewModel.startListening()
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    var backgroundModeBlockedMessage by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(backgroundModeBlockedMessage) {
        if (backgroundModeBlockedMessage) {
            snackbarHostState.showSnackbar("Microphone permission is required to enable Background Mode.")
            backgroundModeBlockedMessage = false
        }
    }

    val quickActions = listOf(
        QuickAction("Check Battery", Icons.Default.BatteryChargingFull, "check battery"),
        QuickAction("Check Storage", Icons.Default.SdCard, "check storage"),
        QuickAction("What Time Is It?", Icons.Default.Schedule, "what time is it"),
        QuickAction("Open Google", Icons.Default.Search, "search for news today"),
        QuickAction("Flashlight On", Icons.Default.FlashlightOn, "turn on flashlight"),
        QuickAction("Mute Volume", Icons.Default.VolumeMute, "mute volume")
    )

    Box(modifier = modifier.fillMaxSize()) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceBlack)
            .padding(horizontal = 20.dp)
            .testTag("home_screen"),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 40.dp)
    ) {
        // Core welcome header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "JAXON VOICE CORES",
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
                        text = "Ready for Orders",
                        style = TextStyle(
                            brush = textGradient,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-1).sp,
                            lineHeight = 34.sp
                        )
                    )
                }

                // Privacy Indicator tag - the status dot breathes slowly to read as a live
                // signal rather than printed text.
                val offlinePulse = rememberInfiniteTransition(label = "offline_pulse")
                val offlineAlpha by offlinePulse.animateFloat(
                    initialValue = 0.6f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing), RepeatMode.Reverse),
                    label = "offline_pulse_alpha"
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = SuccessGreen.copy(alpha = offlineAlpha),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        "OFFLINE",
                        color = SuccessGreen.copy(alpha = offlineAlpha),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Animated Jaxon face trigger
        item {
            val faceState by viewModel.faceState.collectAsState()
            val rmsDb by viewModel.rmsDb.collectAsState()

            var isPressed by remember { mutableStateOf(false) }
            val faceScale by animateFloatAsState(
                targetValue = if (isPressed) 0.96f else 1f,
                animationSpec = tween(80),
                label = "face_tap_scale"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                JaxonFace(
                    state = faceState,
                    rmsDb = rmsDb,
                    size = 160.dp,
                    modifier = Modifier
                        .graphicsLayer(scaleX = faceScale, scaleY = faceScale)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    isPressed = true
                                    tryAwaitRelease()
                                    isPressed = false
                                },
                                onTap = { startListeningWithPermissionCheck() }
                            )
                        }
                        .testTag("home_microphone_button")
                )
            }
        }

        // Quick shortcuts row
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "QUICK ACTIONS",
                    color = SoftTextGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontFamily = FontFamily.Monospace
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(quickActions) { action ->
                        Surface(
                            onClick = { viewModel.executeVoiceText(action.commandText) },
                            color = LightGlassGray,
                            border = BorderStroke(1.dp, GlassBorder),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.testTag("quick_action_${action.label.replace(" ", "_")}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(action.icon, contentDescription = null, tint = GlowCyan, modifier = Modifier.size(16.dp))
                                Text(action.label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Background service toggles card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = LightGlassGray),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, GlassBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(if (isBgActive) SuccessGreen.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.04f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isBgActive) Icons.Default.Power else Icons.Default.PowerOff,
                                contentDescription = null,
                                tint = if (isBgActive) SuccessGreen else SoftTextGray,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Background Mode",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = if (isBgActive) {
                                    "Listening for \"Hey Jaxon\" — uses more battery"
                                } else {
                                    "Hands-free \"Hey Jaxon\" — uses more battery"
                                },
                                color = SoftTextGray,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Switch(
                        checked = isBgActive,
                        onCheckedChange = { enabled ->
                            val micGranted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED

                            if (enabled && !micGranted) {
                                backgroundModeBlockedMessage = true
                            } else {
                                viewModel.toggleBackgroundService(enabled)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = GlowCyan,
                            checkedTrackColor = GlowCyan.copy(alpha = 0.3f),
                            uncheckedThumbColor = SoftTextGray,
                            uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier.testTag("background_service_switch")
                    )
                }
            }
        }

        // Recent commands history
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RECENT CONVERSATIONS",
                        color = SoftTextGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    if (history.isNotEmpty()) {
                        Text(
                            text = "View All",
                            color = GlowCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                // Routes to History tab
                                navController.navigate("history")
                            }
                        )
                    }
                }

                if (history.isEmpty()) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = true,
                        enter = androidx.compose.animation.fadeIn(tween(400)) +
                            androidx.compose.animation.scaleIn(tween(400), initialScale = 0.92f)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DeepGray.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.MicNone, contentDescription = null, tint = SoftTextGray.copy(alpha = 0.5f), modifier = Modifier.size(32.dp))
                                Text(
                                    text = "No recent voice queries",
                                    color = SoftTextGray,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                } else {
                    // Show top 3 recent commands - each row fades + slides up on entry so a
                    // freshly completed command visibly announces itself in the list.
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        history.take(3).forEach { command ->
                            key(command.id) {
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = true,
                                    enter = androidx.compose.animation.fadeIn(tween(350)) +
                                        androidx.compose.animation.slideInVertically(tween(350)) { -it / 3 }
                                ) {
                                    CommandHistoryRow(
                                        command = command,
                                        onFavoriteToggle = { viewModel.toggleFavorite(command) },
                                        onDelete = { viewModel.deleteHistoryItem(command) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}

@Composable
fun CommandHistoryRow(
    command: VoiceCommand,
    onFavoriteToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateString = remember(command.timestamp) {
        val format = SimpleDateFormat("h:mm a", Locale.getDefault())
        format.format(Date(command.timestamp))
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = LightGlassGray),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, GlassBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(GlowCyan.copy(alpha = 0.08f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = command.intentName,
                            color = GlowCyan,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Text(
                        text = dateString,
                        color = SoftTextGray,
                        fontSize = 11.sp
                    )
                }

                Text(
                    text = "\"${command.originalText}\"",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = command.executionResult,
                    color = SoftTextGray,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
            }

            Spacer(Modifier.width(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = onFavoriteToggle,
                    modifier = Modifier.size(34.dp).testTag("star_button_${command.id}")
                ) {
                    Icon(
                        imageVector = if (command.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite Toggle",
                        tint = if (command.isFavorite) GlowCyan else SoftTextGray,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(34.dp).testTag("delete_button_${command.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Command",
                        tint = WarningRed.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private data class QuickAction(
    val label: String,
    val icon: ImageVector,
    val commandText: String
)

private val SoftTextGray = Color(0xFFA0A0AB)
