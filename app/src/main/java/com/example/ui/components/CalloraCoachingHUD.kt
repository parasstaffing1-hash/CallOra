package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Battlecard
import com.example.data.model.LiveCoachingMetrics
import com.example.data.model.LiveTranscriptSnippet
import com.example.ui.theme.*

@Composable
fun TalkToListenRatioCard(
    metrics: LiveCoachingMetrics,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = AgencyNavyCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, AgencyBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = CalloraBlueLight,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Real-Time Callora Coaching",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = AgencyTextPrimary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AgencyEmerald.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = metrics.paceLabel,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AgencyEmerald,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Ratio Bar: You vs Client
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Agency Talk: ${metrics.talkPercentage}%",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CalloraBlueLight
                    )
                    Text(
                        text = "Client Listen: ${metrics.listenPercentage}%",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF25D366)
                    )
                }

                // Dual-color progress bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .weight(metrics.talkPercentage.toFloat().coerceAtLeast(1f))
                            .fillMaxHeight()
                            .background(CalloraBluePrimary)
                    )
                    Box(
                        modifier = Modifier
                            .weight(metrics.listenPercentage.toFloat().coerceAtLeast(1f))
                            .fillMaxHeight()
                            .background(Color(0xFF25D366))
                    )
                }
            }

            // Metrics row: Pacing & Monologue warning
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Speaking Pace: ${metrics.wordsPerMinute} WPM",
                    fontSize = 11.sp,
                    color = AgencyTextSecondary
                )
                Text(
                    text = "Monologue: ${metrics.monologueDurationSec}s",
                    fontSize = 11.sp,
                    color = if (metrics.monologueDurationSec > 40) AgencyAmber else AgencyTextSecondary
                )
            }
        }
    }
}

@Composable
fun ActiveBattlecardPrompt(
    battlecard: Battlecard,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1E1B4B),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, CalloraCoralDot)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = CalloraCoralDot.copy(alpha = 0.2f),
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = CalloraCoralDot,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Text(
                        text = "Callora Battlecard: ${battlecard.title}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Objection detection
            Text(
                text = battlecard.objectionSummary,
                fontSize = 11.5.sp,
                color = CalloraCoralDot,
                fontWeight = FontWeight.SemiBold
            )

            // Recommendation
            Text(
                text = "Recommended Pivot: ${battlecard.recommendedResponse}",
                fontSize = 11.5.sp,
                color = Color.White.copy(alpha = 0.9f),
                lineHeight = 15.sp
            )

            // Quick answer to say
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.Black.copy(alpha = 0.35f),
                border = androidx.compose.foundation.BorderStroke(1.dp, CalloraBlueLight.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SUGGESTED RESPONSE:",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CalloraBlueLight
                        )
                        Text(
                            text = "\"${battlecard.quickAnswer}\"",
                            fontSize = 11.sp,
                            color = Color.White,
                            lineHeight = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(battlecard.quickAnswer))
                            copied = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (copied) AgencyEmerald else CalloraBluePrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text(if (copied) "Copied" else "Copy", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun LiveStreamingTranscriptSection(
    snippets: List<LiveTranscriptSnippet>,
    onSimulateObjection: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = AgencyNavyCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, AgencyBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CalloraGlyph(size = 18.dp)
                    Text(
                        text = "Live Real-Time Transcription",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = AgencyTextPrimary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CalloraBluePrimary.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(AgencyEmerald)
                        )
                        Text(
                            text = "Streaming",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AgencyEmerald
                        )
                    }
                }
            }

            // Snippet bubbles
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                snippets.takeLast(4).forEach { snippet ->
                    val isAgency = snippet.speaker.contains("Agency", ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isAgency) CalloraBluePrimary.copy(alpha = 0.12f) else AgencyNavyDark,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isAgency) CalloraBluePrimary.copy(alpha = 0.3f) else AgencyBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = snippet.speaker,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAgency) CalloraBlueLight else Color(0xFF25D366)
                                )
                                if (snippet.isQuestion) {
                                    Text(
                                        text = "❓ Question Detected",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AgencyAmber
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = snippet.text,
                                fontSize = 11.5.sp,
                                color = AgencyTextPrimary,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }

            // Quick live trigger simulations for testing agency flow
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Test Live AI Objection Prompts:",
                    fontSize = 10.5.sp,
                    color = AgencyTextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SuggestionChip(
                        onClick = { onSimulateObjection("Your quote is higher than our Q3 budget.", "Pricing") },
                        label = { Text("💰 Budget Pushback", fontSize = 10.sp) }
                    )
                    SuggestionChip(
                        onClick = { onSimulateObjection("We want to test this on a 90-day pilot before signing.", "Contract") },
                        label = { Text("📝 Term Pilot", fontSize = 10.sp) }
                    )
                    SuggestionChip(
                        onClick = { onSimulateObjection("How are you different from Competitor X?", "Competitor") },
                        label = { Text("⚔️ Competitor", fontSize = 10.sp) }
                    )
                }
            }
        }
    }
}
