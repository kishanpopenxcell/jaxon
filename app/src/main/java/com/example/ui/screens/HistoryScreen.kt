package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.ui.theme.GlowCyan
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
fun HistoryScreen(
    navController: NavController,
    viewModel: JaxonViewModel,
    modifier: Modifier = Modifier
) {
    val history by viewModel.historyState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showOnlyFavorites by remember { mutableStateOf(false) }

    // Filtered lists
    val filteredHistory = remember(history, searchQuery, showOnlyFavorites) {
        history.filter { command ->
            val matchesQuery = command.originalText.contains(searchQuery, ignoreCase = true) ||
                    command.executionResult.contains(searchQuery, ignoreCase = true) ||
                    command.intentName.contains(searchQuery, ignoreCase = true)
            val matchesFavorite = !showOnlyFavorites || command.isFavorite
            matchesQuery && matchesFavorite
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SpaceBlack)
            .padding(horizontal = 20.dp)
            .testTag("history_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "HISTORY LOGS",
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
                    text = "Command History",
                    style = TextStyle(
                        brush = textGradient,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1).sp,
                        lineHeight = 34.sp
                    )
                )
            }

            if (history.isNotEmpty()) {
                IconButton(
                    onClick = { viewModel.clearHistory() },
                    modifier = Modifier.testTag("clear_history_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ClearAll,
                        contentDescription = "Clear History",
                        tint = WarningRed
                    )
                }
            }
        }

        // Search Bar
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search logs, actions, results...", color = SoftTextGray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SoftTextGray) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = LightGlassGray,
                unfocusedContainerColor = LightGlassGray,
                disabledContainerColor = LightGlassGray,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                .testTag("history_search_input")
        )

        // Filter tabs (All vs Starred)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = !showOnlyFavorites,
                onClick = { showOnlyFavorites = false },
                label = { Text("All Logs (${history.size})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = GlowCyan.copy(alpha = 0.15f),
                    selectedLabelColor = GlowCyan,
                    labelColor = SoftTextGray
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = !showOnlyFavorites,
                    borderColor = Color.White.copy(alpha = 0.08f),
                    selectedBorderColor = GlowCyan.copy(alpha = 0.3f)
                ),
                modifier = Modifier.testTag("all_filter_chip")
            )

            FilterChip(
                selected = showOnlyFavorites,
                onClick = { showOnlyFavorites = true },
                label = { Text("Starred (${history.count { it.isFavorite }})") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (showOnlyFavorites) GlowCyan else SoftTextGray
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = GlowCyan.copy(alpha = 0.15f),
                    selectedLabelColor = GlowCyan,
                    labelColor = SoftTextGray
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = showOnlyFavorites,
                    borderColor = Color.White.copy(alpha = 0.08f),
                    selectedBorderColor = GlowCyan.copy(alpha = 0.3f)
                ),
                modifier = Modifier.testTag("starred_filter_chip")
            )
        }

        // List
        if (filteredHistory.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (showOnlyFavorites) Icons.Default.StarBorder else Icons.Default.History,
                        contentDescription = null,
                        tint = SoftTextGray.copy(alpha = 0.4f),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = if (showOnlyFavorites) "No starred conversations" else "Conversation history is empty",
                        color = SoftTextGray,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("history_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredHistory, key = { it.id }) { command ->
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

private val SoftTextGray = Color(0xFFA0A0AB)
