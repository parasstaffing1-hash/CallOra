package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

// Exact brand colors from Callora logo
val CalloraBluePrimary = Color(0xFF2563EB)
val CalloraBlueLight = Color(0xFF38BDF8)
val CalloraIndigo = Color(0xFF4F46E5)
val CalloraAzureWave = Color(0xFF007AFF)
val CalloraCoralDot = Color(0xFFFF4B4B)
val CalloraNavyWordmark = Color(0xFF0A1128)

/**
 * High-fidelity Vector Canvas of the Callora Logo
 * Replicates the uploaded design: Royal-to-Indigo C arc, 3 electric soundwave bars, and coral dot.
 */
@Composable
fun CalloraGlyph(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeW = w * 0.11f

        // Center of the C-arc circle
        val arcRadius = w * 0.31f
        val arcCenter = Offset(w * 0.44f, h * 0.50f)

        // Arc bounding box
        val arcTopLeft = Offset(arcCenter.x - arcRadius, arcCenter.y - arcRadius)
        val arcSize = Size(arcRadius * 2f, arcRadius * 2f)

        // 1. Stylized C Gradient Arc (sweep ~295 degrees, starting at 35 degrees)
        drawArc(
            brush = Brush.linearGradient(
                colors = listOf(CalloraBluePrimary, CalloraIndigo),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            ),
            startAngle = 35f,
            sweepAngle = 290f,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = strokeW, cap = StrokeCap.Round)
        )

        // 2. Soundwave Bar 1 (Short)
        val bar1X = w * 0.50f
        val bar1HalfH = h * 0.08f
        drawLine(
            color = CalloraAzureWave,
            start = Offset(bar1X, arcCenter.y - bar1HalfH),
            end = Offset(bar1X, arcCenter.y + bar1HalfH),
            strokeWidth = w * 0.055f,
            cap = StrokeCap.Round
        )

        // 3. Soundwave Bar 2 (Tall Center)
        val bar2X = w * 0.60f
        val bar2HalfH = h * 0.17f
        drawLine(
            color = CalloraAzureWave,
            start = Offset(bar2X, arcCenter.y - bar2HalfH),
            end = Offset(bar2X, arcCenter.y + bar2HalfH),
            strokeWidth = w * 0.055f,
            cap = StrokeCap.Round
        )

        // 4. Soundwave Bar 3 (Short)
        val bar3X = w * 0.70f
        val bar3HalfH = h * 0.08f
        drawLine(
            color = CalloraAzureWave,
            start = Offset(bar3X, arcCenter.y - bar3HalfH),
            end = Offset(bar3X, arcCenter.y + bar3HalfH),
            strokeWidth = w * 0.055f,
            cap = StrokeCap.Round
        )

        // 5. Coral-Red Pulse Dot
        val dotX = w * 0.81f
        val dotRadius = w * 0.045f
        drawCircle(
            color = CalloraCoralDot,
            radius = dotRadius,
            center = Offset(dotX, arcCenter.y)
        )
    }
}

/**
 * Top Branding Header with Callora logo and Wordmark
 */
@Composable
fun CalloraBrandHeader(
    modifier: Modifier = Modifier,
    tagline: String = "AI Call Intelligence",
    onLogoClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .then(if (onLogoClick != null) Modifier.clickable { onLogoClick() } else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 2.dp,
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                CalloraGlyph(size = 28.dp)
            }
        }

        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = "Callora",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp,
                    color = AgencyTextPrimary
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = CalloraBluePrimary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "STUDIO",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CalloraBluePrimary,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
            Text(
                text = tagline,
                fontSize = 10.5.sp,
                color = AgencyTextSecondary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Large centered brand presentation with icon and stylized studio badge
 */
@Composable
fun CalloraBrandLogo(
    modifier: Modifier = Modifier,
    logoSize: Dp = 56.dp,
    showWordmark: Boolean = true
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 4.dp,
            modifier = Modifier.size(logoSize)
        ) {
            Box(contentAlignment = Alignment.Center) {
                CalloraGlyph(size = logoSize * 0.72f)
            }
        }
        if (showWordmark) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Callora",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp,
                    color = AgencyTextPrimary
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = CalloraBluePrimary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "STUDIO",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CalloraBluePrimary,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
