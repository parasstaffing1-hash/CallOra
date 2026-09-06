package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Sentiment
import com.example.ui.MainViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val recordings by viewModel.allRecordings.collectAsStateWithLifecycle()
    val totalDurationMs by viewModel.totalDurationMs.collectAsStateWithLifecycle()
    val clients by viewModel.allClients.collectAsStateWithLifecycle()

    val highIntentCallsCount = recordings.count { it.clientSentiment == Sentiment.HIGH_INTENT || it.dealIntentScore >= 80 }
    val totalActionItems = recordings.sumOf { viewModel.repository.parseActionItems(it.actionItemsJson).size }
    val completedActionItems = recordings.sumOf {
        viewModel.repository.parseActionItems(it.actionItemsJson).count { a -> a.isCompleted }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Agency Call Intelligence & Analytics",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AgencyTextPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AgencyNavySurface)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(AgencyNavyDark),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // KPI Stat Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiCard(
                        title = "High Intent Deals",
                        value = "$highIntentCallsCount Calls",
                        subtitle = "Over 80% close probability",
                        color = AgencyEmerald,
                        icon = Icons.Default.RocketLaunch,
                        modifier = Modifier.weight(1f)
                    )

                    KpiCard(
                        title = "Action Item Rate",
                        value = if (totalActionItems > 0) "${(completedActionItems * 100 / totalActionItems)}%" else "100%",
                        subtitle = "$completedActionItems of $totalActionItems resolved",
                        color = AgencyCyan,
                        icon = Icons.Default.TaskAlt,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Sentiment Distribution
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = AgencyNavyCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AgencyBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Client Sentiment Distribution",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = AgencyTextPrimary
                        )

                        SentimentRow(
                            label = "High Intent / Ready to Buy",
                            count = recordings.count { it.clientSentiment == Sentiment.HIGH_INTENT },
                            total = recordings.size,
                            color = AgencyEmerald
                        )
                        SentimentRow(
                            label = "Positive / Retainer Expansion",
                            count = recordings.count { it.clientSentiment == Sentiment.POSITIVE },
                            total = recordings.size,
                            color = AgencyPrimaryLight
                        )
                        SentimentRow(
                            label = "Neutral / Discovery Phase",
                            count = recordings.count { it.clientSentiment == Sentiment.NEUTRAL },
                            total = recordings.size,
                            color = AgencyCyan
                        )
                        SentimentRow(
                            label = "Hesitant / Price Sensitive",
                            count = recordings.count { it.clientSentiment == Sentiment.HESITANT },
                            total = recordings.size,
                            color = AgencyAmber
                        )
                        SentimentRow(
                            label = "At Risk / Escalation",
                            count = recordings.count { it.clientSentiment == Sentiment.AT_RISK },
                            total = recordings.size,
                            color = AgencyRose
                        )
                    }
                }
            }

            // Call Distribution by Stage
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = AgencyNavyCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AgencyBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Agency Call Types Breakdown",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = AgencyTextPrimary
                        )

                        com.example.data.model.CallType.values().forEach { type ->
                            val count = recordings.count { it.callType == type }
                            if (count > 0 || recordings.isEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = type.displayName, fontSize = 13.sp, color = AgencyTextSecondary)
                                    Text(text = "$count calls", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AgencyTextPrimary)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = AgencyNavyCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, AgencyBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = AgencyTextPrimary)
            Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = color)
            Text(text = subtitle, fontSize = 10.5.sp, color = AgencyTextSecondary)
        }
    }
}

@Composable
fun SentimentRow(
    label: String,
    count: Int,
    total: Int,
    color: Color
) {
    val progress = if (total > 0) count.toFloat() / total.toFloat() else 0f
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontSize = 12.sp, color = AgencyTextSecondary)
            Text(text = "$count (${(progress * 100).toInt()}%)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = AgencyBorder
        )
    }
}
