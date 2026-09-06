package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CallRecording
import com.example.data.model.CallType
import com.example.ui.CallFilter
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToActiveRecord: () -> Unit,
    onNavigateToCallDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val recordings by viewModel.allRecordings.collectAsStateWithLifecycle()
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    val recordingTimeMs by viewModel.recordingElapsedTimeMs.collectAsStateWithLifecycle()
    val totalDurationMs by viewModel.totalDurationMs.collectAsStateWithLifecycle()
    val allClients by viewModel.allClients.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val activeFilter by viewModel.activeFilter.collectAsStateWithLifecycle()
    val isPlaying by viewModel.audioPlayerManager.isPlaying.collectAsStateWithLifecycle()

    var showQuickStartDialog by remember { mutableStateOf(false) }
    var isSearchExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(SleekCanvas),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SleekCanvas)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                // Header Row (Callora Studio Brand Layout)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            shadowElevation = 2.dp,
                            modifier = Modifier.size(40.dp)
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
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-0.5).sp,
                                    color = SleekTextPrimary
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
                                text = "Deal Intelligence • ${recordings.size} calls logged",
                                fontSize = 12.sp,
                                color = SleekTextSecondary
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Search Toggle Button (Round 44dp)
                        IconButton(
                            onClick = { isSearchExpanded = !isSearchExpanded },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isSearchExpanded || searchQuery.isNotBlank()) SleekPrimaryContainer else SleekBorderSubtle)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = if (isSearchExpanded || searchQuery.isNotBlank()) SleekOnPrimaryContainer else SleekTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Quick Call Add Button (Round 44dp)
                        IconButton(
                            onClick = { showQuickStartDialog = true },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(SleekBorderSubtle)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Call",
                                tint = SleekTextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // Expandable Search Bar
                AnimatedVisibility(
                    visible = isSearchExpanded || searchQuery.isNotBlank(),
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.searchQuery.value = it },
                            placeholder = { Text("Search clients, transcripts, notes...", fontSize = 13.5.sp, color = SleekTextMuted) },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = "Search", tint = SleekTextMuted, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = SleekTextMuted, modifier = Modifier.size(18.dp))
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_bar"),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SleekSurface,
                                unfocusedContainerColor = SleekSurface,
                                focusedBorderColor = SleekPrimary,
                                unfocusedBorderColor = SleekBorder,
                                focusedTextColor = SleekTextPrimary,
                                unfocusedTextColor = SleekTextPrimary
                            ),
                            singleLine = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Filter Chips Row
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(CallFilter.values()) { filter ->
                        val selected = activeFilter == filter
                        val filterLabel = when (filter) {
                            CallFilter.ALL -> "All (${recordings.size})"
                            CallFilter.WHATSAPP -> "💬 WhatsApp"
                            CallFilter.STARRED -> "⭐ Starred"
                            CallFilter.INBOUND -> "📥 Inbound"
                            CallFilter.OUTBOUND -> "📤 Outbound"
                            CallFilter.HIGH_INTENT -> "🚀 High Intent"
                            CallFilter.HAS_PENDING_ACTIONS -> "📋 Tasks"
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (selected) SleekPrimaryContainer else SleekSurface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (selected) SleekPrimary else SleekBorder
                            ),
                            modifier = Modifier.clickable { viewModel.activeFilter.value = filter }
                        ) {
                            Text(
                                text = filterLabel,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) SleekOnPrimaryContainer else SleekTextSecondary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            // Sleek Squircle FAB (w-14 h-14 rounded-2xl bg-[#0061A4])
            Surface(
                onClick = { showQuickStartDialog = true },
                shape = RoundedCornerShape(16.dp),
                color = SleekPrimary,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .size(56.dp)
                    .testTag("record_call_fab")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Record Call",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(SleekCanvas),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Live Recording Active Banner
            if (isRecording) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToActiveRecord() }
                            .testTag("active_recording_banner"),
                        shape = RoundedCornerShape(16.dp),
                        color = SleekRose.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, SleekRose)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(SleekRose)
                                )
                                Column {
                                    Text(
                                        text = "Recording in Progress...",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SleekRose
                                    )
                                    Text(
                                        text = "Tap to open studio visualizer and bookmark moments",
                                        fontSize = 11.5.sp,
                                        color = SleekTextSecondary
                                    )
                                }
                            }
                            Text(
                                text = formatDuration(recordingTimeMs),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SleekRose
                            )
                        }
                    }
                }
            }

            // Sleek Hero Card (Last Sync / Highlight Card - bg-[#D3E3FD] rounded-[28px])
            if (recordings.isNotEmpty()) {
                val latestCall = recordings.first()
                item {
                    SleekHeroBannerCard(
                        recording = latestCall,
                        onPlayClick = {
                            viewModel.audioPlayerManager.togglePlayPause(
                                latestCall.audioFilePath,
                                latestCall.durationMs
                            )
                        },
                        onCardClick = {
                            viewModel.selectedCallId.value = latestCall.id
                            onNavigateToCallDetail(latestCall.id)
                        }
                    )
                }
            }

            if (recordings.isEmpty()) {
                item {
                    EmptyCallLogsState(
                        isSearching = searchQuery.isNotBlank(),
                        onStartRecording = { showQuickStartDialog = true }
                    )
                }
            } else {
                items(recordings, key = { it.id }) { recording ->
                    CallRecordingItemCard(
                        recording = recording,
                        onCardClick = {
                            viewModel.selectedCallId.value = recording.id
                            onNavigateToCallDetail(recording.id)
                        },
                        onToggleStar = { viewModel.toggleStarred(recording.id, recording.isStarred) },
                        onQuickPlay = {
                            viewModel.audioPlayerManager.togglePlayPause(
                                recording.audioFilePath,
                                recording.durationMs
                            )
                        },
                        modifier = Modifier.testTag("call_item_${recording.id}")
                    )
                }
            }

            // Bottom Spacing for FAB
            item {
                Spacer(modifier = Modifier.height(84.dp))
            }
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    if (showQuickStartDialog) {
        QuickStartCallDialog(
            clients = allClients,
            onDismiss = { showQuickStartDialog = false },
            onStartCall = { clientName, company, phone, callType, direction, platform ->
                showQuickStartDialog = false
                if (platform.equals("WhatsApp", ignoreCase = true)) {
                    viewModel.startLiveRecording(
                        clientName = clientName,
                        clientCompany = company,
                        phone = phone,
                        callType = callType,
                        direction = direction,
                        platform = "WhatsApp",
                        audioSourcePref = "VOICE_COMMUNICATION"
                    )
                    if (phone.isNotBlank()) {
                        viewModel.launchWhatsApp(context, phone)
                    }
                } else {
                    viewModel.startLiveRecording(
                        clientName = clientName,
                        clientCompany = company,
                        phone = phone,
                        callType = callType,
                        direction = direction,
                        platform = "Phone"
                    )
                }
                onNavigateToActiveRecord()
            }
        )
    }

    val isFloatingWidgetEnabled by viewModel.isFloatingWidgetEnabled.collectAsStateWithLifecycle()
    val isPaused by viewModel.isRecordingPaused.collectAsStateWithLifecycle()
    val currentPlatform by viewModel.activeCallPlatform.collectAsStateWithLifecycle()

    if (isRecording && isFloatingWidgetEnabled) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 90.dp, end = 16.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            CalloraFloatingInCallWidget(
                isRecording = isRecording,
                isPaused = isPaused,
                elapsedTimeMs = recordingTimeMs,
                platform = currentPlatform,
                onAddBookmark = { category, title ->
                    viewModel.addLiveBookmark(category, title, "Tagged from In-Call Widget")
                },
                onPauseResume = {
                    if (isPaused) viewModel.resumeRecording() else viewModel.pauseRecording()
                },
                onOpenFullApp = onNavigateToActiveRecord,
                onTriggerBattlecard = { tag ->
                    viewModel.triggerBattlecardForTag(tag)
                    onNavigateToActiveRecord()
                }
            )
        }
    }
}

@Composable
fun SleekHeroBannerCard(
    recording: CallRecording,
    onPlayClick: () -> Unit,
    onCardClick: () -> Unit
) {
    val dateStr = remember(recording.timestamp) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(recording.timestamp))
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        shape = RoundedCornerShape(28.dp),
        color = SleekPrimaryContainer,
        border = androidx.compose.foundation.BorderStroke(1.dp, SleekPrimary.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "LAST SYNC",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    color = SleekOnPrimaryContainer
                )
                Text(
                    text = recording.clientCompany.ifBlank { recording.clientName },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = SleekOnPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Today • $dateStr • ${formatDuration(recording.durationMs)}",
                    fontSize = 13.sp,
                    color = SleekOnPrimaryContainer.copy(alpha = 0.75f)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // White circular play action button
            Surface(
                onClick = onPlayClick,
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play recording",
                        tint = SleekPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AgencyOverviewBanner(
    totalCalls: Int,
    totalDurationMs: Long,
    activeClientsCount: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = AgencyNavyCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, AgencyBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MetricCol(
                value = totalCalls.toString(),
                label = "Agency Calls",
                icon = Icons.Default.GraphicEq,
                iconColor = AgencyPrimaryLight
            )

            Divider(
                modifier = Modifier
                    .height(36.dp)
                    .width(1.dp),
                color = AgencyBorder
            )

            val totalMinutes = totalDurationMs / 60000
            val totalHours = totalMinutes / 60
            val durationLabel = if (totalHours > 0) "${totalHours}h ${totalMinutes % 60}m" else "${totalMinutes}m"
            MetricCol(
                value = durationLabel,
                label = "Recorded Time",
                icon = Icons.Default.AccessTime,
                iconColor = AgencyCyan
            )

            Divider(
                modifier = Modifier
                    .height(36.dp)
                    .width(1.dp),
                color = AgencyBorder
            )

            MetricCol(
                value = activeClientsCount.toString(),
                label = "Key Clients",
                icon = Icons.Default.Business,
                iconColor = AgencyEmerald
            )
        }
    }
}

@Composable
fun MetricCol(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(16.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = AgencyTextPrimary
            )
        }
        Text(
            text = label,
            fontSize = 11.sp,
            color = AgencyTextSecondary
        )
    }
}

@Composable
fun CallRecordingItemCard(
    recording: CallRecording,
    onCardClick: () -> Unit,
    onToggleStar: () -> Unit,
    onQuickPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateStr = remember(recording.timestamp) {
        SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.getDefault()).format(Date(recording.timestamp))
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        shape = RoundedCornerShape(16.dp),
        color = SleekSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Avatar, Client Name/Company, Duration & Sync status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ClientAvatar(
                    name = recording.clientName,
                    company = recording.clientCompany,
                    size = 48
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recording.clientCompany.ifBlank { recording.clientName },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SleekTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${recording.callType.displayName} • $dateStr",
                        fontSize = 12.sp,
                        color = SleekTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = formatDuration(recording.durationMs),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SleekTextPrimary
                    )
                    Icon(
                        imageVector = if (recording.isTranscribed) Icons.Default.CloudDone else Icons.Default.CloudSync,
                        contentDescription = "Synced",
                        tint = SleekPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Chips: Platform (WhatsApp / Phone), Call Type, Direction, Sentiment
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (recording.platform.equals("WhatsApp", ignoreCase = true)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF25D366).copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF25D366).copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = "WhatsApp",
                                tint = Color(0xFF075E54),
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "WhatsApp",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF075E54)
                            )
                        }
                    }
                }
                CallTypeChip(callType = recording.callType)
                CallDirectionIcon(direction = recording.direction)
                Spacer(modifier = Modifier.weight(1f))
                SentimentBadge(sentiment = recording.clientSentiment)
            }

            // AI Summary Snippet or Notes
            val displaySnippet = when {
                recording.aiSummary.isNotBlank() -> recording.aiSummary
                recording.notes.isNotBlank() -> recording.notes
                else -> "Call recording saved. Tap to view AI transcripts, key takeaways, and action items."
            }

            Text(
                text = displaySnippet,
                fontSize = 12.5.sp,
                color = SleekTextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 17.sp
            )

            HorizontalDivider(color = SleekBorderSubtle)

            // Footer: AI Status + Action Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (recording.isAnalyzing) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                color = SleekPrimary,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "Analyzing AI...",
                                fontSize = 11.sp,
                                color = SleekPrimary
                            )
                        }
                    } else if (recording.isTranscribed) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SleekPrimaryContainer
                        ) {
                            Text(
                                text = "✨ AI Ready",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = SleekOnPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onToggleStar,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (recording.isStarred) Icons.Default.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Star Call",
                            tint = if (recording.isStarred) SleekAmber else SleekTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Button(
                    onClick = onCardClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SleekPrimaryContainer,
                        contentColor = SleekOnPrimaryContainer
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("View Intelligence", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyCallLogsState(
    isSearching: Boolean,
    onStartRecording: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        shape = RoundedCornerShape(20.dp),
        color = SleekSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorder)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(SleekPrimaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSearching) Icons.Default.SearchOff else Icons.Default.MicNone,
                    contentDescription = null,
                    tint = SleekOnPrimaryContainer,
                    modifier = Modifier.size(28.dp)
                )
            }

            Text(
                text = if (isSearching) "No calls matched your search" else "No agency calls recorded yet",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = SleekTextPrimary
            )

            Text(
                text = if (isSearching) "Try a different search keyword or clear filters."
                else "Tap the Record button below or initiate a client session to capture call intelligence.",
                fontSize = 12.5.sp,
                color = SleekTextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 18.sp
            )

            if (!isSearching) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onStartRecording,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SleekPrimary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Start Call Recording", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickStartCallDialog(
    clients: List<com.example.data.model.AgencyClient>,
    onDismiss: () -> Unit,
    onStartCall: (String, String, String, CallType, com.example.data.model.CallDirection, String) -> Unit
) {
    var selectedClient by remember { mutableStateOf<com.example.data.model.AgencyClient?>(clients.firstOrNull()) }
    var clientName by remember { mutableStateOf(clients.firstOrNull()?.name ?: "") }
    var clientCompany by remember { mutableStateOf(clients.firstOrNull()?.company ?: "") }
    var clientPhone by remember { mutableStateOf(clients.firstOrNull()?.phone ?: "") }
    var callType by remember { mutableStateOf(CallType.DISCOVERY) }
    var direction by remember { mutableStateOf(com.example.data.model.CallDirection.OUTBOUND) }
    var platform by remember { mutableStateOf("WhatsApp") } // Default to WhatsApp / Phone
    var isNewClientMode by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Record Agency Call", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SleekTextPrimary)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Platform Selector (WhatsApp vs Phone)
                Text("Call Platform:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SleekTextSecondary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isWA = platform == "WhatsApp"
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isWA) Color(0xFF25D366).copy(alpha = 0.16f) else SleekSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(
                            if (isWA) 1.5.dp else 1.dp,
                            if (isWA) Color(0xFF25D366) else SleekBorder
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { platform = "WhatsApp" }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = null,
                                tint = if (isWA) Color(0xFF128C7E) else SleekTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "WhatsApp Call",
                                fontSize = 12.sp,
                                fontWeight = if (isWA) FontWeight.Bold else FontWeight.Medium,
                                color = if (isWA) Color(0xFF128C7E) else SleekTextPrimary
                            )
                        }
                    }

                    val isPhone = platform == "Phone"
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isPhone) SleekPrimaryContainer else SleekSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(
                            if (isPhone) 1.5.dp else 1.dp,
                            if (isPhone) SleekPrimary else SleekBorder
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { platform = "Phone" }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = if (isPhone) SleekPrimary else SleekTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Regular Phone",
                                fontSize = 12.sp,
                                fontWeight = if (isPhone) FontWeight.Bold else FontWeight.Medium,
                                color = if (isPhone) SleekOnPrimaryContainer else SleekTextPrimary
                            )
                        }
                    }
                }

                if (platform == "WhatsApp") {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF25D366).copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF25D366).copy(alpha = 0.25f))
                    ) {
                        Text(
                            text = "🟢 VoIP Audio Engine: Hardware echo cancellation active. Keep speakerphone on or use headset for crystal-clear two-way WhatsApp call recording.",
                            fontSize = 11.sp,
                            color = Color(0xFF075E54),
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                // Client Selector Chips
                if (clients.isNotEmpty() && !isNewClientMode) {
                    Text("Select Agency Client:", fontSize = 12.sp, color = SleekTextSecondary)
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(clients) { client ->
                            val isSelected = selectedClient?.id == client.id
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedClient = client
                                    clientName = client.name
                                    clientCompany = client.company
                                    clientPhone = client.phone
                                },
                                label = { Text(client.company, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SleekPrimaryContainer,
                                    selectedLabelColor = SleekOnPrimaryContainer,
                                    containerColor = SleekSurfaceVariant,
                                    labelColor = SleekTextSecondary
                                )
                            )
                        }
                    }
                    TextButton(
                        onClick = {
                            isNewClientMode = true
                            selectedClient = null
                            clientName = ""
                            clientCompany = ""
                            clientPhone = ""
                        },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("+ Or enter new client details", fontSize = 12.sp, color = SleekPrimary)
                    }
                }

                if (isNewClientMode || clients.isEmpty()) {
                    OutlinedTextField(
                        value = clientCompany,
                        onValueChange = { clientCompany = it },
                        label = { Text("Company / Account Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = { Text("Contact Person Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = clientPhone,
                        onValueChange = { clientPhone = it },
                        label = { Text("Phone / WhatsApp Number (+...)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                // Call Type
                Text("Call Objective:", fontSize = 12.sp, color = SleekTextSecondary)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(CallType.values().take(5)) { type ->
                        val isSelected = callType == type
                        FilterChip(
                            selected = isSelected,
                            onClick = { callType = type },
                            label = { Text(type.displayName, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SleekPrimaryContainer,
                                selectedLabelColor = SleekOnPrimaryContainer
                            )
                        )
                    }
                }

                // Direction
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = direction == com.example.data.model.CallDirection.OUTBOUND,
                        onClick = { direction = com.example.data.model.CallDirection.OUTBOUND },
                        label = { Text("📤 Outbound Call", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = direction == com.example.data.model.CallDirection.INBOUND,
                        onClick = { direction = com.example.data.model.CallDirection.INBOUND },
                        label = { Text("📥 Inbound Call", fontSize = 11.sp) }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onStartCall(
                        clientName.ifBlank { "Client" },
                        clientCompany.ifBlank { "Agency Partner" },
                        clientPhone,
                        callType,
                        direction,
                        platform
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (platform == "WhatsApp") Color(0xFF128C7E) else SleekPrimary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("confirm_start_recording")
            ) {
                val buttonText = if (platform == "WhatsApp" && clientPhone.isNotBlank()) "Record & Open WhatsApp"
                else if (platform == "WhatsApp") "Record WhatsApp Call"
                else "Start Recording"
                Text(buttonText, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SleekTextSecondary)
            }
        },
        containerColor = SleekSurface,
        shape = RoundedCornerShape(24.dp)
    )
}

fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
