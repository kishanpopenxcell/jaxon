package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.ui.theme.GlowCyan
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.SpaceBlack
import com.example.ui.viewmodel.JaxonViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    navController: NavController,
    viewModel: JaxonViewModel,
    modifier: Modifier = Modifier
) {
    val scale = remember { Animatable(0.6f) }
    val alpha = remember { Animatable(0f) }
    val onboardingCompleted by viewModel.isOnboardingCompleted.collectAsState()

    LaunchedEffect(key1 = true) {
        // Run animations in parallel
        scale.animateTo(1.0f, animationSpec = tween(1000))
        alpha.animateTo(1.0f, animationSpec = tween(1000))
        delay(1200)

        // Navigate based on preferences state
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
        // Background glow
        Box(
            modifier = Modifier
                .size(300.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            ElectricPurple.copy(alpha = 0.12f),
                            GlowCyan.copy(alpha = 0.04f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .scale(scale.value)
                .testTag("splash_content")
        ) {
            // Futuristic AI Waveform Launcher Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color.White.copy(alpha = 0.05f), androidx.compose.foundation.shape.CircleShape)
                    .border(2.dp, Brush.linearGradient(listOf(GlowCyan, ElectricPurple)), androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = "Jaxon Logo",
                    tint = GlowCyan,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

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

private val SoftTextGray = Color(0xFFA0A0AB)
