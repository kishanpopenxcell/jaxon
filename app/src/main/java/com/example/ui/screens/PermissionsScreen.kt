package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.domain.permissions.PermissionItem
import com.example.domain.permissions.PermissionProvider
import com.example.domain.permissions.PermissionType
import com.example.ui.theme.GlowCyan
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.DeepGray
import com.example.ui.theme.SpaceBlack
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningRed
import com.example.ui.theme.LightGlassGray
import com.example.ui.theme.GlassBorder
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Brush
import com.example.ui.viewmodel.JaxonViewModel

@Composable
fun PermissionsScreen(
    navController: NavController,
    viewModel: JaxonViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var permissionList by remember { mutableStateOf(PermissionProvider.getPermissions()) }

    // Map to track grant states locally
    val permissionStates = remember { mutableStateMapOf<String, Boolean>() }

    // Tracks permissions that were denied at least once, so we can tell a fresh
    // "not asked yet" state apart from a permanent "Don't ask again" denial.
    val deniedOnceIds = remember { mutableStateMapOf<String, Boolean>() }

    fun isPermanentlyDenied(item: PermissionItem): Boolean {
        if (item.type != PermissionType.RUNTIME || item.permissionString == null) return false
        val granted = permissionStates[item.id] ?: false
        if (granted) return false
        if (deniedOnceIds[item.id] != true) return false
        val shouldShowRationale = activity?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(it, item.permissionString)
        } ?: false
        return !shouldShowRationale
    }

    // Re-check statuses
    fun updateStatuses() {
        permissionList.forEach { item ->
            val isGranted = when (item.type) {
                PermissionType.RUNTIME -> {
                    item.permissionString?.let {
                        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                    } ?: false
                }
                PermissionType.SYSTEM_ALERT -> {
                    Settings.canDrawOverlays(context)
                }
                PermissionType.BATTERY_OPTIMIZATION -> {
                    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                    pm.isIgnoringBatteryOptimizations(context.packageName)
                }
            }
            permissionStates[item.id] = isGranted
        }
    }

    // Trigger initial check
    LaunchedEffect(Unit) {
        updateStatuses()
    }

    // Track active item being requested for standard runtime permission launcher
    var activePermissionItem by remember { mutableStateOf<PermissionItem?>(null) }

    val runtimeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        activePermissionItem?.let { item ->
            permissionStates[item.id] = isGranted
            if (!isGranted) {
                deniedOnceIds[item.id] = true
            }
        }
        updateStatuses()
    }

    fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}")
        )
        context.startActivity(intent)
    }

    // Request permissions router
    fun requestPermission(item: PermissionItem) {
        if (isPermanentlyDenied(item)) {
            openAppSettings()
            return
        }
        activePermissionItem = item
        when (item.type) {
            PermissionType.RUNTIME -> {
                item.permissionString?.let {
                    runtimeLauncher.launch(it)
                }
            }
            PermissionType.SYSTEM_ALERT -> {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                context.startActivity(intent)
            }
            PermissionType.BATTERY_OPTIMIZATION -> {
                val intent = Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:${context.packageName}")
                )
                context.startActivity(intent)
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(SpaceBlack),
        bottomBar = {
            Surface(
                color = LightGlassGray,
                border = BorderStroke(1.dp, GlassBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .windowInsetsPadding(WindowInsets.navigationBars),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val micGranted = permissionStates["microphone"] ?: false

                    if (!micGranted) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = WarningRed, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Microphone Access is required to start Jaxon.",
                                color = WarningRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    val gradientSweep by animateFloatAsState(
                        targetValue = if (micGranted) 1f else 0f,
                        animationSpec = tween(350),
                        label = "finish_button_gradient"
                    )
                    Button(
                        onClick = {
                            // Completes onboarding and opens Home Screen
                            viewModel.completeOnboarding()
                            navController.navigate("home") {
                                popUpTo("permissions") { inclusive = true }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.12f)
                        ),
                        enabled = micGranted,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        GlowCyan.copy(alpha = gradientSweep),
                                        ElectricPurple.copy(alpha = gradientSweep)
                                    )
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .testTag("permissions_done_button")
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Finish Setup and Go Home",
                                color = if (micGranted) Color.Black else SoftTextGray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = if (micGranted) Color.Black else SoftTextGray)
                        }
                    }
                }
            }
        },
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SpaceBlack)
                    .padding(horizontal = 24.dp, vertical = 20.dp)
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                Text(
                    text = "PERMISSIONS CENTER",
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
                    text = "Establish Voice Control",
                    style = TextStyle(
                        brush = textGradient,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1).sp,
                        lineHeight = 34.sp
                    )
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Review permissions below. Jaxon is fully local and never shares or transmits private data.",
                    color = SoftTextGray,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(SpaceBlack)
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("permissions_list"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Re-check action item at list start
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { updateStatuses() },
                        colors = ButtonDefaults.textButtonColors(contentColor = GlowCyan)
                    ) {
                        Text("Re-scan Permissions", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            itemsIndexed(permissionList) { index, item ->
                val granted = permissionStates[item.id] ?: false
                val permanentlyDenied = isPermanentlyDenied(item)

                var visible by remember(item.id) { mutableStateOf(false) }
                LaunchedEffect(item.id) {
                    kotlinx.coroutines.delay(index * 70L)
                    visible = true
                }

                val borderColor by animateColorAsState(
                    targetValue = if (granted) SuccessGreen.copy(alpha = 0.25f) else GlassBorder,
                    animationSpec = tween(250),
                    label = "permission_border"
                )

                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 3 }
                ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (granted) LightGlassGray else LightGlassGray.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(width = 1.dp, color = borderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Title row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = item.purpose,
                                    color = SoftTextGray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (item.isRequired) GlowCyan.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f))
                                    .border(1.dp, if (item.isRequired) GlowCyan.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (item.isRequired) "REQUIRED" else "OPTIONAL",
                                    color = if (item.isRequired) GlowCyan else SoftTextGray,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        // Explanation
                        Text(
                            text = item.explanation,
                            color = SoftTextGray.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        // Status & Control Button Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Status tag
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val dotColor by animateColorAsState(
                                    targetValue = if (granted) SuccessGreen else WarningRed,
                                    animationSpec = tween(250),
                                    label = "status_dot"
                                )
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(dotColor)
                                )
                                AnimatedContent(
                                    targetState = when {
                                        granted -> "Active"
                                        permanentlyDenied -> "Blocked"
                                        else -> "Pending Grant"
                                    },
                                    transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) },
                                    label = "status_text"
                                ) { label ->
                                    Text(
                                        text = label,
                                        color = if (granted) SuccessGreen else WarningRed,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Button
                            Button(
                                onClick = { requestPermission(item) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (granted) Color.White.copy(alpha = 0.05f) else ElectricPurple
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !granted,
                                modifier = Modifier
                                    .height(38.dp)
                                    .testTag("grant_button_${item.id}")
                            ) {
                                AnimatedContent(
                                    targetState = when {
                                        granted -> "Granted"
                                        permanentlyDenied -> "Open Settings"
                                        else -> "Grant"
                                    },
                                    transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) },
                                    label = "grant_button_text"
                                ) { label ->
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (granted) SoftTextGray else Color.White
                                    )
                                }
                            }
                        }
                    }
                }
                }
            }
        }
    }
}

private val SoftTextGray = Color(0xFFA0A0AB)
