package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = GlowCyan,
    secondary = ElectricPurple,
    tertiary = DeepViolet,
    background = SpaceBlack,
    surface = DeepGray,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = IceBlue,
    onSurface = IceBlue,
    surfaceVariant = DeepGray,
    onSurfaceVariant = SoftTextGray,
    error = WarningRed,
    onError = Color.Black
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF00ACC1),
    secondary = ElectricPurple,
    tertiary = DeepViolet,
    background = Color(0xFFF4F6F9),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = SpaceBlack,
    onSurface = SpaceBlack,
    surfaceVariant = Color(0xFFEBEFF5),
    onSurfaceVariant = Color(0xFF5A5A66),
    error = WarningRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force Dark theme by default for the premium futuristic aesthetic
    dynamicColor: Boolean = false, // Disable dynamic colors to preserve our tailored neon vibe
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

