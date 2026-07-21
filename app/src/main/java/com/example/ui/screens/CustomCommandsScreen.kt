package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.data.model.CustomRoutine
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
fun CustomCommandsScreen(
    navController: NavController,
    viewModel: JaxonViewModel,
    modifier: Modifier = Modifier
) {
    val routines by viewModel.routinesState.collectAsState()

    var showCreatorForm by remember { mutableStateOf(false) }
    var triggerText by remember { mutableStateOf("") }
    var actionsText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun handleSave() {
        if (triggerText.isBlank()) {
            errorMessage = "Please enter a voice trigger phrase."
            return
        }
        if (actionsText.isBlank()) {
            errorMessage = "Please enter at least one action."
            return
        }
        viewModel.addRoutine(triggerText, actionsText)
        triggerText = ""
        actionsText = ""
        errorMessage = null
        showCreatorForm = false
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SpaceBlack)
            .padding(horizontal = 20.dp)
            .testTag("custom_commands_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 40.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "LOCAL AUTOMATIONS",
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
                        text = "Custom Routines",
                        style = TextStyle(
                            brush = textGradient,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-1).sp,
                            lineHeight = 34.sp
                        )
                    )
                }

                IconButton(
                    onClick = { showCreatorForm = !showCreatorForm },
                    modifier = Modifier
                        .background(GlowCyan.copy(alpha = 0.1f), CircleShape)
                        .testTag("add_routine_toggle_button")
                ) {
                    Icon(
                        imageVector = if (showCreatorForm) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = "Create Routine",
                        tint = GlowCyan
                    )
                }
            }
        }

        // Animated Form to Create Routines
        item {
            AnimatedVisibility(
                visible = showCreatorForm,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = LightGlassGray),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, GlowCyan.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "NEW AUTOMATION",
                            color = GlowCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        // Trigger phrase
                        OutlinedTextField(
                            value = triggerText,
                            onValueChange = { triggerText = it },
                            label = { Text("When I say...", color = SoftTextGray) },
                            placeholder = { Text("e.g. work mode", color = SoftTextGray.copy(alpha = 0.5f)) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = GlowCyan,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.12f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("routine_trigger_input")
                        )

                        // Actions separated by commas
                        OutlinedTextField(
                            value = actionsText,
                            onValueChange = { actionsText = it },
                            label = { Text("Execute these commands...", color = SoftTextGray) },
                            placeholder = { Text("e.g. open gmail, open calendar, increase volume", color = SoftTextGray.copy(alpha = 0.5f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = GlowCyan,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.12f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("routine_actions_input")
                        )

                        Text(
                            text = "Separate multiple commands with commas. System will analyze and run each step sequentially.",
                            color = SoftTextGray,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )

                        if (errorMessage != null) {
                            Text(
                                text = errorMessage ?: "",
                                color = WarningRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { showCreatorForm = false },
                                colors = ButtonDefaults.textButtonColors(contentColor = SoftTextGray)
                            ) {
                                Text("Cancel")
                            }

                            Spacer(Modifier.width(8.dp))

                            Button(
                                onClick = { handleSave() },
                                colors = ButtonDefaults.buttonColors(containerColor = GlowCyan),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("save_routine_button")
                            ) {
                                Text("Save Routine", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Info / Guide item (if empty)
        if (routines.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DeepGray.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = SoftTextGray.copy(alpha = 0.5f),
                            modifier = Modifier.size(38.dp)
                        )
                        Text(
                            text = "No automations registered",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Routines allow you to run multiple actions with a single spoken keyword phrase. Tap the '+' button at the top to configure your first command script.",
                            color = SoftTextGray,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            // Routines list
            items(routines, key = { it.id }) { routine ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (routine.isEnabled) LightGlassGray else LightGlassGray.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (routine.isFavorite) GlowCyan.copy(alpha = 0.3f) else GlassBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("routine_card_${routine.id}")
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Title / Toggle Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = routine.triggerPhrase.uppercase(),
                                    color = if (routine.isEnabled) GlowCyan else SoftTextGray,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Keyword trigger phrase",
                                    color = SoftTextGray,
                                    fontSize = 11.sp
                                )
                            }

                            Switch(
                                checked = routine.isEnabled,
                                onCheckedChange = { viewModel.toggleRoutineEnabled(routine) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = GlowCyan,
                                    checkedTrackColor = GlowCyan.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.testTag("routine_toggle_switch_${routine.id}")
                            )
                        }

                        // Steps list
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "AUTOMATION STEPS:",
                                color = SoftTextGray,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )

                            val steps = routine.actionsJson.split(",").map { it.trim() }
                            steps.forEachIndexed { index, step ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .background(ElectricPurple.copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = (index + 1).toString(),
                                            color = ElectricPurple,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Text(
                                        text = step,
                                        color = if (routine.isEnabled) Color.White else SoftTextGray,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Action Buttons Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Favorite toggle
                            IconButton(
                                onClick = { viewModel.toggleRoutineFavorite(routine) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = if (routine.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "Favorite Automation",
                                    tint = if (routine.isFavorite) GlowCyan else SoftTextGray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Duplicate & Delete controls
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(
                                    onClick = { viewModel.duplicateRoutine(routine) },
                                    colors = ButtonDefaults.textButtonColors(contentColor = SoftTextGray),
                                    modifier = Modifier.testTag("routine_duplicate_button_${routine.id}")
                                ) {
                                    Icon(Icons.Default.CopyAll, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Duplicate", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                TextButton(
                                    onClick = { viewModel.deleteRoutine(routine) },
                                    colors = ButtonDefaults.textButtonColors(contentColor = WarningRed.copy(alpha = 0.8f)),
                                    modifier = Modifier.testTag("routine_delete_button_${routine.id}")
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Delete", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
