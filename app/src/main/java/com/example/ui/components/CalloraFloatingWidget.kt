package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun CalloraFloatingInCallWidget(
    isRecording: Boolean,
    isPaused: Boolean,
    elapsedTimeMs: Long,
    platform: String,
    onAddBookmark: (String, String) -> Unit,
    onPauseResume: () -> Unit,
    onOpenFullApp: () -> Unit,
    onTriggerBattlecard: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var isExpanded by remember { mutableStateOf(false) }

    val formattedTime = remember(elapsedTimeMs) {
        val totalSec = elapsedTimeMs / 1000
        val m = totalSec / 60
        val s = totalSec % 60
        String.format("%02d:%02d", m, s)
    }

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            }
            .shadow(12.dp, RoundedCornerShape(24.dp))
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A).copy(alpha = 0.96f),
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                if (platform.equals("WhatsApp", ignoreCase = true)) Color(0xFF25D366) else CalloraBluePrimary
            )
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Collapsed Pill Bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.clickable { isExpanded = !isExpanded }
                ) {
                    CalloraGlyph(size = 22.dp)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isPaused) AgencyAmber else AgencyEmerald)
                        )
                        Text(
                            text = formattedTime,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (platform.equals("WhatsApp", ignoreCase = true)) Color(0xFF25D366).copy(alpha = 0.2f) else CalloraBluePrimary.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = if (platform.equals("WhatsApp", ignoreCase = true)) "WA" else "Call",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (platform.equals("WhatsApp", ignoreCase = true)) Color(0xFF25D366) else CalloraBlueLight,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = AgencyTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Expanded In-Call Controls
                AnimatedVisibility(visible = isExpanded) {
                    Column(
                        modifier = Modifier
                            .widthIn(min = 220.dp)
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Divider(color = AgencyBorder)

                        Text(
                            text = "Quick In-Call Bookmarks:",
                            fontSize = 10.sp,
                            color = AgencyTextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Button(
                                onClick = { onAddBookmark("Pricing", "Pricing discussed") },
                                colors = ButtonDefaults.buttonColors(containerColor = AgencyNavyDark),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(26.dp)
                            ) {
                                Text("💰 Price", fontSize = 9.sp, color = Color.White)
                            }
                            Button(
                                onClick = { onAddBookmark("Agreement", "Client verbal agreement") },
                                colors = ButtonDefaults.buttonColors(containerColor = AgencyNavyDark),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(26.dp)
                            ) {
                                Text("🤝 Agree", fontSize = 9.sp, color = Color.White)
                            }
                            Button(
                                onClick = { onAddBookmark("Action Item", "Action item noted") },
                                colors = ButtonDefaults.buttonColors(containerColor = AgencyNavyDark),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(26.dp)
                            ) {
                                Text("📋 Task", fontSize = 9.sp, color = Color.White)
                            }
                        }

                        // Battlecard Quick Trigger
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onTriggerBattlecard("Pricing") },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(26.dp)
                            ) {
                                Text("Budget Card", fontSize = 9.sp, color = CalloraCoralDot)
                            }
                            OutlinedButton(
                                onClick = { onTriggerBattlecard("Timeline") },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(26.dp)
                            ) {
                                Text("Timeline Card", fontSize = 9.sp, color = CalloraBlueLight)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onPauseResume,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    contentDescription = null,
                                    tint = if (isPaused) AgencyEmerald else AgencyAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Button(
                                onClick = onOpenFullApp,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CalloraBluePrimary,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Text("Open Callora ↗", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
