package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.ui.components.JaxonFace
import com.example.ui.components.JaxonFaceState
import com.example.ui.components.rememberJaxonIntroAnimation
import com.example.ui.theme.SpaceBlack
import com.example.ui.viewmodel.JaxonViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    navController: NavController,
    viewModel: JaxonViewModel,
    modifier: Modifier = Modifier
) {
    val onboardingCompleted by viewModel.isOnboardingCompleted.collectAsState()
    val intro = rememberJaxonIntroAnimation()

    var showText by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = true) {
        // Waves converge (~1.1s) -> settle (~0.4s) -> wink (~0.5s) -> reveal text once the
        // whole entrance has visibly finished, matching the approved motion review timing.
        delay(2200)
        showText = true
        delay(1600)

        if (onboardingCompleted) {
            navController.navigate("home") {
                popUpTo("splash") { inclusive = true }
            }
        } else {
            navController.navigate("onboarding") {
                popUpTo("splash") { inclusive = true }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SpaceBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.testTag("splash_content")
        ) {
            JaxonFace(
                state = JaxonFaceState.IDLE,
                size = 160.dp,
                intro = intro
            )

            Spacer(modifier = Modifier.height(8.dp))

            AnimatedVisibility(
                visible = showText,
                enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { it / 3 }
            ) {
                val jaxonGradient = Brush.linearGradient(
                    colors = listOf(Color.White, Color(0xFF94A3B8))
                )
                Text(
                    text = "JAXON",
                    style = androidx.compose.ui.text.TextStyle(
                        brush = jaxonGradient,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 8.sp
                    )
                )
            }

            AnimatedVisibility(
                visible = showText,
                enter = fadeIn(tween(600, delayMillis = 150)) + slideInVertically(tween(600, delayMillis = 150)) { it / 3 }
            ) {
                Text(
                    text = "OFFLINE VOICE INTELLIGENCE",
                    color = SoftTextGray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

private val SoftTextGray = Color(0xFFA0A0AB)
