package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NetworkWifi3Bar
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.ui.theme.GlowCyan
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.DeepGray
import com.example.ui.theme.SpaceBlack
import com.example.ui.viewmodel.JaxonViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    navController: NavController,
    viewModel: JaxonViewModel,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()

    val pages = listOf(
        OnboardingPageData(
            title = "Absolute Privacy",
            description = "Jaxon handles 100% of speech recognition, parsing, and execution locally. No cloud servers, no analytics, and zero external tracking. Your data is truly yours.",
            icon = Icons.Default.PrivacyTip,
            accentColor = GlowCyan
        ),
        OnboardingPageData(
            title = "Native Android Power",
            description = "Control your device with natural offline speech. Open installed apps, check available storage, call contacts, set alarms, toggle your flashlight, and manage volume directly.",
            icon = Icons.Default.Mic,
            accentColor = ElectricPurple
        ),
        OnboardingPageData(
            title = "Custom Automations",
            description = "Build custom multi-step routines. Say 'Movie Time' to launch streaming apps, connect bluetooth, and turn down the volume instantly. Automate anything offline.",
            icon = Icons.Default.Tune,
            accentColor = GlowCyan
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SpaceBlack)
            .padding(24.dp)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // App Header
        Text(
            text = "JAXON ASSISTANT",
            color = GlowCyan.copy(alpha = 0.6f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )

        // Horizontal Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("onboarding_pager")
        ) { pageIndex ->
            val page = pages[pageIndex]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Feature Icon
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(page.accentColor.copy(alpha = 0.08f), CircleShape)
                        .border(1.dp, page.accentColor.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = page.icon,
                        contentDescription = null,
                        tint = page.accentColor,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Title
                val textGradient = Brush.linearGradient(
                    colors = listOf(Color.White, Color(0xFF94A3B8))
                )
                Text(
                    text = page.title,
                    style = androidx.compose.ui.text.TextStyle(
                        brush = textGradient,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1).sp,
                        lineHeight = 32.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Description
                Text(
                    text = page.description,
                    color = SoftTextGray,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        // Bottom section (Indicators & Buttons)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Page Indicator dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (i in 0 until 3) {
                    val active = pagerState.currentPage == i
                    Box(
                        modifier = Modifier
                            .size(height = 6.dp, width = if (active) 24.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (active) GlowCyan else Color.White.copy(alpha = 0.15f))
                    )
                }
            }

            // Next / Complete Button
            val isLastPage = pagerState.currentPage == 2
            Button(
                onClick = {
                    if (isLastPage) {
                        viewModel.completeOnboarding()
                        // Route straight to permissions to set them up
                        navController.navigate("permissions") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    } else {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = if (isLastPage) GlowCyan else DeepGray),
                shape = RoundedCornerShape(16.dp),
                border = if (isLastPage) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("onboarding_next_button")
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isLastPage) "Review Permissions" else "Continue",
                        color = if (isLastPage) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = if (isLastPage) Color.Black else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private data class OnboardingPageData(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: Color
)

private val SoftTextGray = Color(0xFFA0A0AB)
