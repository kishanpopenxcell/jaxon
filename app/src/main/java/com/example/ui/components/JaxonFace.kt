package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Visual state of Jaxon's animated face. Maps 1:1 to the wave-character design:
 * each state has distinct amplitude/speed/color, not just a color swap.
 */
enum class JaxonFaceState {
    IDLE,
    LISTENING,
    PROCESSING,
    SUCCESS,
    ERROR
}

private data class WaveLayer(
    val radiusFraction: Float,
    val amp1: Float,
    val f1: Float,
    val amp2: Float,
    val f2: Float,
    val s1: Float,
    val s2: Float,
    val widthFraction: Float,
    val color: Color,
    val opacity: Float
)

private val CyanSet = listOf(Color(0xFF67E8F9), Color(0xFFA855F7), Color(0xFF8B5CF6))
private val PurpleSet = listOf(Color(0xFF8B5CF6), Color(0xFFA855F7), Color(0xFFC084FC))
private val GreenSet = listOf(Color(0xFF6EE7B7), Color(0xFF10B981), Color(0xFF34D399))
private val RedSet = listOf(Color(0xFFFCA5A5), Color(0xFFEF4444), Color(0xFFDC2626))

private fun waveLayers(
    ampScale: Float,
    radiusScale: Float,
    colors: List<Color>,
    opacityScale: Float
): List<WaveLayer> = listOf(
    WaveLayer(0.325f * radiusScale, 0.042f * ampScale, 5f, 0.021f * ampScale, 3f, 1f, 0.6f, 0.019f, colors[0], 0.9f * opacityScale),
    WaveLayer(0.383f * radiusScale, 0.033f * ampScale, 4f, 0.025f * ampScale, 7f, -0.7f, 0.5f, 0.015f, colors[1], 0.75f * opacityScale),
    WaveLayer(0.275f * radiusScale, 0.025f * ampScale, 8f, 0.0125f * ampScale, 5f, 0.9f, -1.1f, 0.0125f, colors[2], 0.6f * opacityScale)
)

/**
 * Drives the one-time Splash entrance: waves converge inward from the screen edges, the visor
 * fades in as they settle, then a single asymmetric wink (right eye closes first, holds, both
 * reopen together) plays once. Pass to [JaxonFace] only on the Splash screen; other screens
 * omit it and render the steady-state loop for [state] immediately.
 */
class JaxonIntroAnimation {
    val convergeProgress = androidx.compose.runtime.mutableFloatStateOf(0f)
    val faceProgress = androidx.compose.runtime.mutableFloatStateOf(0f)

    // -1 = not started yet, 0..1 = wink in progress, 2 = finished (eyes back to normal loop).
    val winkProgress = androidx.compose.runtime.mutableFloatStateOf(-1f)

    internal suspend fun run() {
        val converge = Animatable(0f)
        val face = Animatable(0f)
        // Waves converge first (ease-out cubic), face fades in slightly before they finish.
        kotlinx.coroutines.coroutineScope {
            launch {
                converge.animateTo(1f, tween(1100, easing = CubicBezierEasing(0.33f, 1f, 0.68f, 1f))) {
                    convergeProgress.floatValue = value
                }
            }
            launch {
                delay(500)
                face.animateTo(1f, tween(600, easing = LinearEasing)) {
                    faceProgress.floatValue = value
                }
            }
        }
        convergeProgress.floatValue = 1f
        faceProgress.floatValue = 1f

        delay(400)
        // Wink: right eye closes, holds, then both reopen together.
        winkProgress.floatValue = 0f
        val wink = Animatable(0f)
        wink.animateTo(1f, tween(500, easing = LinearEasing)) {
            winkProgress.floatValue = value
        }
        winkProgress.floatValue = 2f
    }
}

@Composable
fun rememberJaxonIntroAnimation(): JaxonIntroAnimation {
    val intro = remember { JaxonIntroAnimation() }
    LaunchedEffect(Unit) { intro.run() }
    return intro
}

/**
 * Jaxon's animated face: a Canvas-drawn fluid-wave aura around a flat visor with two glowing
 * eye bars. Behavior (amplitude/speed/color of the wave, eye shape) is entirely driven by
 * [state], so callers only need to pass the current app state - no separate animation wiring
 * per screen.
 *
 * @param rmsDb live microphone level (0f..~10f, see SpeechManager.rmsDb) used to drive eye
 * height while [state] is [JaxonFaceState.LISTENING].
 * @param intro when non-null (Splash only), plays the one-time converge-in + wink sequence
 * from [rememberJaxonIntroAnimation] before settling into the normal [state] loop.
 */
@Composable
fun JaxonFace(
    state: JaxonFaceState,
    modifier: Modifier = Modifier,
    size: Dp = 104.dp,
    rmsDb: Float = 0f,
    intro: JaxonIntroAnimation? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "jaxon_face_phase")

    val phaseSpeed = when (state) {
        JaxonFaceState.IDLE -> 5200
        JaxonFaceState.LISTENING -> 1400
        JaxonFaceState.PROCESSING -> 950
        JaxonFaceState.SUCCESS -> 2200
        JaxonFaceState.ERROR -> 9000
    }

    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(phaseSpeed, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    // Success is a one-shot decaying bloom rather than a steady loop - a separate 0..1
    // transition that restarts whenever we (re-)enter SUCCESS, driving amplitude/opacity falloff.
    val successTrigger = remember(state) { state }
    val successProgress by animateFloatAsState(
        targetValue = if (successTrigger == JaxonFaceState.SUCCESS) 1f else 0f,
        animationSpec = tween(700, easing = LinearEasing),
        label = "success_bloom"
    )

    val layers = remember(state) {
        when (state) {
            JaxonFaceState.IDLE -> waveLayers(0.55f, 1f, CyanSet, 1f)
            JaxonFaceState.LISTENING -> waveLayers(1.4f, 0.94f, CyanSet, 1f)
            JaxonFaceState.PROCESSING -> waveLayers(0.35f, 0.82f, PurpleSet, 0.9f)
            JaxonFaceState.SUCCESS -> waveLayers(0.4f, 1f, GreenSet, 0.6f)
            JaxonFaceState.ERROR -> waveLayers(0.15f, 1.02f, RedSet, 0.6f)
        }
    }

    val ambientColor = when (state) {
        JaxonFaceState.IDLE -> Color(0xFFA855F7).copy(alpha = 0.14f)
        JaxonFaceState.LISTENING -> Color(0xFFA855F7).copy(alpha = 0.28f)
        JaxonFaceState.PROCESSING -> Color(0xFF8B5CF6).copy(alpha = 0.22f)
        JaxonFaceState.SUCCESS -> Color(0xFF10B981).copy(alpha = 0.1f + 0.35f * (1f - successProgress))
        JaxonFaceState.ERROR -> Color(0xFFEF4444).copy(alpha = 0.12f)
    }

    val eyeColors = when (state) {
        JaxonFaceState.ERROR -> listOf(Color(0xFFFCA5A5), Color(0xFFEF4444))
        JaxonFaceState.PROCESSING -> listOf(Color(0xFF22D3EE), Color(0xFF8B5CF6))
        JaxonFaceState.SUCCESS -> listOf(Color(0xFF6EE7B7), Color(0xFF10B981))
        else -> listOf(Color(0xFFB4F1FF), Color(0xFF22D3EE))
    }

    val convergeT = intro?.convergeProgress?.floatValue ?: 1f
    val faceT = intro?.faceProgress?.floatValue ?: 1f
    val winkT = intro?.winkProgress?.floatValue ?: 2f

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f
        val faceRadius = w * 0.30f

        // Ambient glow ramps in alongside the wave convergence so it never appears before
        // there's anything visible to glow around.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(ambientColor.copy(alpha = ambientColor.alpha * convergeT), Color.Transparent),
                center = Offset(cx, cy),
                radius = w * 0.5f
            ),
            radius = w * 0.5f,
            center = Offset(cx, cy)
        )

        // Success bloom multiplies amplitude/opacity by a decaying spike on top of the base layer
        val successSpike = if (state == JaxonFaceState.SUCCESS) (1f - successProgress) else 0f

        for (layer in layers) {
            val ampBoost = if (state == JaxonFaceState.SUCCESS) 1f + 1.4f * successSpike else 1f
            // Splash entrance: waves start collapsed onto the face edge (radius == faceRadius,
            // zero amplitude) and ease outward/ripple in as convergeT goes 0 -> 1.
            val radius = faceRadius + (w * layer.radiusFraction - faceRadius) * convergeT
            val amp1 = w * layer.amp1 * ampBoost * convergeT
            val amp2 = w * layer.amp2 * ampBoost * convergeT
            val strokeWidth = w * layer.widthFraction

            val path = Path()
            val steps = 96
            for (i in 0..steps) {
                val theta = (i / steps.toFloat()) * 2f * PI.toFloat()
                val r = radius +
                    amp1 * sin(layer.f1 * theta + phase * layer.s1) +
                    amp2 * sin(layer.f2 * theta - phase * layer.s2)
                val x = cx + r * cos(theta)
                val y = cy + r * sin(theta)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()

            val opacity = if (state == JaxonFaceState.SUCCESS) {
                (layer.opacity * (0.5f + 0.5f * successSpike)).coerceIn(0f, 1f)
            } else {
                layer.opacity
            } * convergeT

            drawPath(
                path = path,
                color = layer.color.copy(alpha = opacity),
                style = Stroke(width = strokeWidth)
            )
        }

        // Flat visor face - fades in slightly ahead of the waves finishing their convergence.
        drawCircle(color = Color(0xFF0A0A0F).copy(alpha = faceT), radius = faceRadius, center = Offset(cx, cy))

        if (faceT > 0.01f) {
            drawEyes(
                state = state,
                cx = cx,
                cy = cy,
                w = w,
                phase = phase,
                rmsDb = rmsDb,
                colors = eyeColors,
                alpha = faceT,
                winkProgress = winkT
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEyes(
    state: JaxonFaceState,
    cx: Float,
    cy: Float,
    w: Float,
    phase: Float,
    rmsDb: Float,
    colors: List<Color>,
    alpha: Float = 1f,
    winkProgress: Float = 2f
) {
    val eyeWidth = w * 0.075f
    val gap = w * 0.145f
    val baseHeight = w * 0.115f
    val brush = Brush.verticalGradient(colors)

    when (state) {
        JaxonFaceState.PROCESSING -> {
            // Three chasing "thinking" dots
            val dotRadius = w * 0.026f
            val spacing = w * 0.09f
            for (d in -1..1) {
                val dPhase = sin(phase * 3f + d * 2f)
                val alpha = (0.35f + 0.65f * (0.5f + 0.5f * dPhase)).coerceIn(0f, 1f)
                drawCircle(
                    color = colors[1].copy(alpha = alpha),
                    radius = dotRadius,
                    center = Offset(cx + d * spacing, cy)
                )
            }
        }
        JaxonFaceState.SUCCESS -> {
            // Brief upward "happy squint" curve
            val curveWidth = w * 0.028f
            for (side in listOf(-1f, 1f)) {
                val eyeCx = cx + side * gap
                val path = Path().apply {
                    moveTo(eyeCx - eyeWidth * 0.6f, cy + baseHeight * 0.15f)
                    quadraticTo(eyeCx, cy - baseHeight * 0.55f, eyeCx + eyeWidth * 0.6f, cy + baseHeight * 0.15f)
                }
                drawPath(
                    path = path,
                    color = colors[1],
                    style = Stroke(width = curveWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
            }
        }
        JaxonFaceState.ERROR -> {
            // Eyes tilt inward/downward - apologetic look
            for (side in listOf(-1f, 1f)) {
                val eyeCx = cx + side * gap
                val rotation = -14f * side
                withTransform({
                    rotate(degrees = rotation, pivot = Offset(eyeCx, cy))
                }) {
                    drawRoundRect(
                        brush = brush,
                        topLeft = Offset(eyeCx - eyeWidth / 2f, cy - baseHeight / 2f),
                        size = androidx.compose.ui.geometry.Size(eyeWidth, baseHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(eyeWidth / 2f, eyeWidth / 2f)
                    )
                }
            }
        }
        JaxonFaceState.LISTENING -> {
            // Bars bounce with live mic volume
            val volumeScale = (rmsDb / 10f).coerceIn(0f, 1f)
            val heightL = baseHeight * (1.2f + 0.9f * volumeScale + 0.15f * sin(phase * 4f))
            val heightR = baseHeight * (1.2f + 0.9f * volumeScale + 0.15f * sin(phase * 4f + PI.toFloat()))
            drawRoundRect(
                brush = brush,
                topLeft = Offset(cx - gap - eyeWidth / 2f, cy - heightL / 2f),
                size = androidx.compose.ui.geometry.Size(eyeWidth, heightL),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(eyeWidth / 2f, eyeWidth / 2f)
            )
            drawRoundRect(
                brush = brush,
                topLeft = Offset(cx + gap - eyeWidth / 2f, cy - heightR / 2f),
                size = androidx.compose.ui.geometry.Size(eyeWidth, heightR),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(eyeWidth / 2f, eyeWidth / 2f)
            )
        }
        JaxonFaceState.IDLE -> {
            val breathe = 0.9f + 0.06f * sin(phase * 0.6f)

            // Splash intro wink: right eye closes first, holds, then both reopen together.
            // winkProgress: 0..1 while the wink plays, 2 once finished (falls through to the
            // normal idle breathing height on both sides).
            var heightL = baseHeight * breathe
            var heightR = baseHeight * breathe
            if (winkProgress in 0f..1f) {
                val closePhase = (winkProgress / 0.5f).coerceIn(0f, 1f)
                val reopenPhase = ((winkProgress - 0.65f) / 0.35f).coerceIn(0f, 1f)
                heightR = if (winkProgress < 0.65f) {
                    baseHeight * (1f - 0.92f * sin(closePhase * (PI.toFloat() / 2f)))
                } else {
                    baseHeight * (0.08f + 0.92f * reopenPhase)
                }
            }

            drawRoundRect(
                brush = brush,
                topLeft = Offset(cx - gap - eyeWidth / 2f, cy - heightL / 2f),
                size = androidx.compose.ui.geometry.Size(eyeWidth, heightL),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(eyeWidth / 2f, eyeWidth / 2f),
                alpha = 0.7f * alpha
            )
            drawRoundRect(
                brush = brush,
                topLeft = Offset(cx + gap - eyeWidth / 2f, cy - heightR / 2f),
                size = androidx.compose.ui.geometry.Size(eyeWidth, heightR),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(eyeWidth / 2f, eyeWidth / 2f),
                alpha = 0.7f * alpha
            )
        }
    }
}
