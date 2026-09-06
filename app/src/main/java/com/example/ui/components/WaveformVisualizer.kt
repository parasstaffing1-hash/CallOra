package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CallBookmark
import com.example.ui.theme.*

@Composable
fun LiveRecordingWaveform(
    amplitudes: List<Float>,
    decibels: Float,
    isPaused: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave_pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_anim"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // VU Meter / Decibel readout
        Row(
            modifier = Modifier.padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isPaused) AgencyNavyCard else AgencyRose.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isPaused) AgencyBorder else AgencyRose.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .then(
                                if (!isPaused) Modifier else Modifier
                            )
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(color = if (isPaused) AgencyAmber else AgencyRose)
                        }
                    }
                    Text(
                        text = if (isPaused) "PAUSED" else "LIVE ${decibels.toInt()} dB",
                        fontSize = 11.sp,
                        color = if (isPaused) AgencyAmber else AgencyRose,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Text(
                text = "HD 44.1kHz AAC",
                fontSize = 11.sp,
                color = AgencyCyan,
                style = MaterialTheme.typography.labelSmall
            )
        }

        // Animated Audio Waveform Bars
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
        ) {
            val width = size.width
            val height = size.height
            val barCount = 36
            val barWidth = (width / barCount) * 0.55f
            val spacing = (width / barCount) * 0.45f
            val centerY = height / 2f

            val displayList = if (amplitudes.size >= barCount) {
                amplitudes.takeLast(barCount)
            } else {
                val pad = List(barCount - amplitudes.size) { 0.15f }
                pad + amplitudes
            }

            for (i in 0 until barCount) {
                val amp = displayList.getOrElse(i) { 0.2f }
                val scale = if (!isPaused) (if (i % 2 == 0) pulse else 1f) else 1f
                val barHeight = ((amp * height * 0.85f) * scale).coerceIn(6f, height - 4f)
                val x = i * (barWidth + spacing) + spacing / 2
                val top = centerY - (barHeight / 2f)

                val barColor = if (isPaused) {
                    AgencyTextMuted
                } else {
                    if (i > barCount - 6) AgencyRose else AgencyPrimaryLight
                }

                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(4f, 4f)
                )
            }
        }
    }
}

@Composable
fun InteractiveAudioWaveform(
    amplitudes: List<Float>,
    currentPosMs: Long,
    totalDurationMs: Long,
    bookmarks: List<CallBookmark> = emptyList(),
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (totalDurationMs > 0) (currentPosMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f) else 0f
    
    // Sample 50 bars for high fidelity scrubber
    val barCount = 50
    val sampledAmps = remember(amplitudes) {
        if (amplitudes.isEmpty()) {
            List(barCount) { 0.25f + ((it % 5) * 0.12f) }
        } else {
            val step = amplitudes.size.toFloat() / barCount
            List(barCount) { index ->
                val targetIdx = (index * step).toInt().coerceIn(0, amplitudes.size - 1)
                amplitudes[targetIdx]
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(84.dp)
            .pointerInput(totalDurationMs) {
                detectTapGestures { offset ->
                    val ratio = (offset.x / size.width).coerceIn(0f, 1f)
                    val targetMs = (ratio * totalDurationMs).toLong()
                    onSeek(targetMs)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val barWidth = (width / barCount) * 0.6f
            val spacing = (width / barCount) * 0.4f
            val centerY = height / 2f
            val activeX = width * progress

            for (i in 0 until barCount) {
                val x = i * (barWidth + spacing) + spacing / 2
                val amp = sampledAmps[i]
                val barHeight = (amp * height * 0.75f).coerceIn(8f, height - 12f)
                val top = centerY - (barHeight / 2f)

                val isPlayed = x <= activeX

                drawRoundRect(
                    color = if (isPlayed) AgencyCyan else AgencyBorder,
                    topLeft = Offset(x, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(4f, 4f)
                )
            }

            // Draw Playhead scrubber line
            drawLine(
                color = Color.White,
                start = Offset(activeX, 2f),
                end = Offset(activeX, height - 2f),
                strokeWidth = 3f
            )
            drawCircle(
                color = AgencyCyan,
                radius = 6f,
                center = Offset(activeX, height - 4f)
            )

            // Draw Bookmarks / Moments flags
            bookmarks.forEach { bookmark ->
                if (totalDurationMs > 0) {
                    val bmRatio = (bookmark.timestampMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
                    val bmX = width * bmRatio
                    drawCircle(
                        color = AgencyAmber,
                        radius = 4f,
                        center = Offset(bmX, 6f)
                    )
                }
            }
        }
    }
}
