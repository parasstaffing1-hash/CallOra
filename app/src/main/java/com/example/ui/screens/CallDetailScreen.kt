package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ActionItem
import com.example.data.model.CallRecording
import com.example.data.model.TranscriptSegment
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

enum class DetailTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    SUMMARY("AI Summary", Icons.Outlined.AutoAwesome),
    ACTION_ITEMS("Action Items", Icons.Outlined.Checklist),
    TRANSCRIPT("Transcript", Icons.Outlined.FormatQuote),
    EMAIL_DRAFT("Follow-up Email", Icons.Outlined.Email),
    NOTES("Notes & Markers", Icons.Outlined.BookmarkBorder)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallDetailScreen(
    callId: Long,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val call by viewModel.selectedCall.collectAsStateWithLifecycle()

    val isPlaying by viewModel.audioPlayerManager.isPlaying.collectAsStateWithLifecycle()
    val currentPositionMs by viewModel.audioPlayerManager.currentPositionMs.collectAsStateWithLifecycle()
    val totalDurationMs by viewModel.audioPlayerManager.totalDurationMs.collectAsStateWithLifecycle()
    val playbackSpeed by viewModel.audioPlayerManager.playbackSpeed.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(DetailTab.SUMMARY) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showCrmExportDialog by remember { mutableStateOf(false) }

    val keyTakeaways = remember(call?.keyTakeawaysJson) {
        call?.let { viewModel.repository.parseKeyTakeaways(it.keyTakeawaysJson) } ?: emptyList()
    }

    val actionItems = remember(call?.actionItemsJson) {
        call?.let { viewModel.repository.parseActionItems(it.actionItemsJson) } ?: emptyList()
    }

    val transcriptSegments = remember(call?.transcriptSegmentsJson) {
        call?.let { viewModel.repository.parseTranscriptSegments(it.transcriptSegmentsJson) } ?: emptyList()
    }

    val bookmarks = remember(call?.bookmarksJson) {
        call?.let { viewModel.repository.parseBookmarks(it.bookmarksJson) } ?: emptyList()
    }

    val waveform = remember(call?.waveformAmplitudesJson) {
        call?.let { viewModel.repository.parseWaveform(it.waveformAmplitudesJson) } ?: emptyList()
    }

    val dateFormatted = remember(call?.timestamp) {
        call?.let { SimpleDateFormat("EEEE, MMM dd, yyyy • h:mm a", Locale.getDefault()).format(Date(it.timestamp)) } ?: ""
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = call?.clientCompany?.ifBlank { "Call Intelligence" } ?: "Call Intelligence",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = AgencyTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = AgencyTextPrimary)
                    }
                },
                actions = {
                    // Star button
                    call?.let { currentCall ->
                        IconButton(onClick = { viewModel.toggleStarred(currentCall.id, currentCall.isStarred) }) {
                            Icon(
                                imageVector = if (currentCall.isStarred) Icons.Default.Star else Icons.Outlined.StarBorder,
                                contentDescription = "Star",
                                tint = if (currentCall.isStarred) AgencyAmber else AgencyTextSecondary
                            )
                        }
                    }

                    // WhatsApp Share button
                    call?.let { currentCall ->
                        IconButton(onClick = {
                            val textToShare = "AgencyCall Intelligence Recap: ${currentCall.title}\nClient: ${currentCall.clientName} (${currentCall.clientCompany})\n\nSummary:\n${currentCall.aiSummary}\n\nKey Action Items:\n${actionItems.joinToString("\n") { "• [${if (it.isCompleted) "x" else " "}] ${it.task} (${it.assignee})" }}"
                            viewModel.shareViaWhatsApp(context, textToShare, currentCall.clientPhone)
                        }) {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = "Share via WhatsApp",
                                tint = Color(0xFF25D366)
                            )
                        }
                    }

                    // Callora CRM & Deliverables Export button
                    IconButton(onClick = { showCrmExportDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = "CRM & Deliverables Export",
                            tint = CalloraBlueLight
                        )
                    }

                    // Share button
                    IconButton(onClick = {
                        call?.let { currentCall ->
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Callora Executive Recap: ${currentCall.title}")
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    viewModel.generateCalloraRecap(currentCall)
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Callora Executive Recap"))
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = AgencyTextSecondary)
                    }

                    // Delete button
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AgencyTextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AgencyNavySurface)
            )
        }
    ) { paddingValues ->
        if (call == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AgencyPrimary)
            }
        } else {
            val currentCall = call!!

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(AgencyNavyDark),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Client Card & AI Deal Intent Gauge
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ClientAvatar(
                                    name = currentCall.clientName,
                                    company = currentCall.clientCompany,
                                    size = 46
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = currentCall.clientCompany.ifBlank { currentCall.clientName },
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AgencyTextPrimary
                                    )
                                    Text(
                                        text = "${currentCall.clientName} ${if (currentCall.clientPhone.isNotBlank()) "• ${currentCall.clientPhone}" else ""}",
                                        fontSize = 12.sp,
                                        color = AgencyTextSecondary
                                    )
                                    Text(
                                        text = dateFormatted,
                                        fontSize = 11.sp,
                                        color = AgencyTextMuted
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (currentCall.platform.equals("WhatsApp", ignoreCase = true)) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF25D366).copy(alpha = 0.15f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF25D366).copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Chat,
                                                contentDescription = null,
                                                tint = Color(0xFF25D366),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = "WhatsApp VoIP",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF25D366)
                                            )
                                        }
                                    }
                                }
                                CallTypeChip(callType = currentCall.callType)
                                CallDirectionIcon(direction = currentCall.direction)
                                Spacer(modifier = Modifier.weight(1f))
                                SentimentBadge(sentiment = currentCall.clientSentiment)
                            }

                            // Audio Channel & Direct WhatsApp Launcher
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Engine: ${currentCall.audioSourceUsed}",
                                    fontSize = 11.sp,
                                    color = AgencyTextMuted
                                )
                                if (currentCall.clientPhone.isNotBlank()) {
                                    TextButton(
                                        onClick = { viewModel.launchWhatsApp(context, currentCall.clientPhone) },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("Open in WhatsApp ↗", fontSize = 11.5.sp, color = Color(0xFF25D366), fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            // Deal Intent Score Bar
                            if (currentCall.dealIntentScore > 0) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = AgencyNavyDark,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
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
                                                tint = AgencyEmerald,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "Deal Buying Intent Score:",
                                                fontSize = 11.5.sp,
                                                color = AgencyTextSecondary
                                            )
                                        }

                                        Text(
                                            text = "${currentCall.dealIntentScore} / 100",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (currentCall.dealIntentScore >= 80) AgencyEmerald else AgencyAmber
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Studio Audio Player Card
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = AgencyNavySurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AgencyBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Player Title Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Studio Playback & Scrubber",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AgencyTextSecondary
                                )
                                Text(
                                    text = "${formatDuration(currentPositionMs)} / ${formatDuration(if (totalDurationMs > 0) totalDurationMs else currentCall.durationMs)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AgencyCyan
                                )
                            }

                            // Interactive Waveform
                            InteractiveAudioWaveform(
                                amplitudes = waveform,
                                currentPosMs = currentPositionMs,
                                totalDurationMs = if (totalDurationMs > 0) totalDurationMs else currentCall.durationMs,
                                bookmarks = bookmarks,
                                onSeek = { targetMs -> viewModel.audioPlayerManager.seekTo(targetMs) }
                            )

                            // Controls Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Speed Selector Pill
                                val nextSpeed = when (playbackSpeed) {
                                    1.0f -> 1.25f
                                    1.25f -> 1.5f
                                    1.5f -> 2.0f
                                    2.0f -> 0.75f
                                    else -> 1.0f
                                }

                                TextButton(
                                    onClick = { viewModel.audioPlayerManager.setSpeed(nextSpeed) },
                                    colors = ButtonDefaults.textButtonColors(contentColor = AgencyCyan),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "${playbackSpeed}x Speed",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Rewind / Play / Forward
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = { viewModel.audioPlayerManager.rewind10Seconds() },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Replay10,
                                            contentDescription = "Rewind 10s",
                                            tint = AgencyTextPrimary
                                        )
                                    }

                                    FilledIconButton(
                                        onClick = {
                                            viewModel.audioPlayerManager.togglePlayPause(
                                                currentCall.audioFilePath,
                                                currentCall.durationMs
                                            )
                                        },
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = AgencyPrimary,
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier.size(50.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = if (isPlaying) "Pause" else "Play",
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.audioPlayerManager.forward10Seconds() },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Forward10,
                                            contentDescription = "Forward 10s",
                                            tint = AgencyTextPrimary
                                        )
                                    }
                                }

                                // Re-run AI Analysis button
                                IconButton(
                                    onClick = { viewModel.triggerAIAnalysisForCall(currentCall.id) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.AutoAwesome,
                                        contentDescription = "Re-analyze with Gemini",
                                        tint = AgencyPrimaryLight
                                    )
                                }
                            }
                        }
                    }
                }

                // Tab Bar
                item {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(DetailTab.values()) { tab ->
                            val isSelected = selectedTab == tab
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedTab = tab },
                                label = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = tab.icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = if (isSelected) Color.White else AgencyTextSecondary
                                        )
                                        Text(
                                            text = tab.label + if (tab == DetailTab.ACTION_ITEMS && actionItems.isNotEmpty()) " (${actionItems.size})" else "",
                                            fontSize = 12.sp
                                        )
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AgencyPrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = AgencyNavyCard,
                                    labelColor = AgencyTextSecondary
                                )
                            )
                        }
                    }
                }

                // Tab Content Rendering
                when (selectedTab) {
                    DetailTab.SUMMARY -> {
                        item {
                            SummaryTabContent(
                                call = currentCall,
                                keyTakeaways = keyTakeaways,
                                onReanalyze = { viewModel.triggerAIAnalysisForCall(currentCall.id) }
                            )
                        }
                    }
                    DetailTab.ACTION_ITEMS -> {
                        item {
                            ActionItemsTabContent(
                                callId = currentCall.id,
                                actionItems = actionItems,
                                onToggleAction = { actionId -> viewModel.toggleActionItem(currentCall.id, actionId) }
                            )
                        }
                    }
                    DetailTab.TRANSCRIPT -> {
                        item {
                            TranscriptTabContent(
                                segments = transcriptSegments,
                                fullTranscript = currentCall.transcript,
                                onSeekTo = { timestampMs ->
                                    viewModel.audioPlayerManager.seekTo(timestampMs)
                                    if (!isPlaying) {
                                        viewModel.audioPlayerManager.prepareAndPlay(currentCall.audioFilePath, currentCall.durationMs)
                                    }
                                }
                            )
                        }
                    }
                    DetailTab.EMAIL_DRAFT -> {
                        item {
                            EmailDraftTabContent(
                                emailDraft = currentCall.followUpEmailDraft,
                                onCopy = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Follow-up Email", currentCall.followUpEmailDraft))
                                    Toast.makeText(context, "Email draft copied to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                onShare = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "Follow-up: ${currentCall.clientCompany}")
                                        putExtra(Intent.EXTRA_TEXT, currentCall.followUpEmailDraft)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Send Client Email"))
                                }
                            )
                        }
                    }
                    DetailTab.NOTES -> {
                        item {
                            NotesAndBookmarksTabContent(
                                notes = currentCall.notes,
                                bookmarks = bookmarks,
                                onSeekTo = { timestampMs ->
                                    viewModel.audioPlayerManager.seekTo(timestampMs)
                                    if (!isPlaying) {
                                        viewModel.audioPlayerManager.prepareAndPlay(currentCall.audioFilePath, currentCall.durationMs)
                                    }
                                }
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(36.dp))
                }
            }
        }
    }

    if (showCrmExportDialog && call != null) {
        CalloraCrmExportDialog(
            call = call!!,
            viewModel = viewModel,
            onDismiss = { showCrmExportDialog = false }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Call Recording?", color = AgencyTextPrimary) },
            text = { Text("This will permanently remove the audio recording, transcripts, and AI notes from your agency repository.", color = AgencyTextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        call?.let { viewModel.deleteRecording(it) }
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AgencyRose)
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = AgencyTextPrimary)
                }
            },
            containerColor = AgencyNavySurface
        )
    }
}

@Composable
fun SummaryTabContent(
    call: CallRecording,
    keyTakeaways: List<String>,
    onReanalyze: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Executive Summary Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = AgencyNavyCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, AgencyBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = AgencyPrimaryLight,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Executive Summary",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = AgencyTextPrimary
                        )
                    }

                    if (call.isAnalyzing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = AgencyPrimaryLight,
                            strokeWidth = 2.dp
                        )
                    }
                }

                Text(
                    text = call.aiSummary.ifBlank { "Analysis will generate key takeaways and actionable executive summary." },
                    fontSize = 13.5.sp,
                    color = AgencyTextPrimary,
                    lineHeight = 20.sp
                )
            }
        }

        // Key Takeaways List
        if (keyTakeaways.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = AgencyNavyCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, AgencyBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Key Decisions & Discussion Points",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = AgencyTextPrimary
                    )

                    keyTakeaways.forEach { takeaway ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text("⚡", fontSize = 13.sp)
                            Text(
                                text = takeaway,
                                fontSize = 13.sp,
                                color = AgencyTextSecondary,
                                lineHeight = 18.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Deal Intelligence & Key Objections
        if (call.dealSizeEstimate.isNotBlank() || call.keyObjections.isNotBlank()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = AgencyNavyCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, AgencyBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Deal Risk & Commercial Intel",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = AgencyTextPrimary
                    )

                    if (call.dealSizeEstimate.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Estimated Contract Value:", fontSize = 12.sp, color = AgencyTextSecondary)
                            Text(call.dealSizeEstimate, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AgencyEmerald)
                        }
                    }

                    if (call.keyObjections.isNotBlank()) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Identified Hesitations / Objections:", fontSize = 12.sp, color = AgencyTextSecondary)
                            Text(call.keyObjections, fontSize = 12.5.sp, color = AgencyAmber)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActionItemsTabContent(
    callId: Long,
    actionItems: List<ActionItem>,
    onToggleAction: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (actionItems.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = AgencyNavyCard
            ) {
                Text(
                    text = "No action items extracted for this call yet.",
                    fontSize = 13.sp,
                    color = AgencyTextSecondary,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            val completedCount = actionItems.count { it.isCompleted }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = AgencyPrimary.copy(alpha = 0.12f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Progress: $completedCount of ${actionItems.size} completed",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AgencyPrimaryLight
                    )
                    LinearProgressIndicator(
                        progress = { if (actionItems.isNotEmpty()) completedCount.toFloat() / actionItems.size else 0f },
                        modifier = Modifier
                            .width(100.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = AgencyEmerald,
                        trackColor = AgencyBorder
                    )
                }
            }

            actionItems.forEach { item ->
                ActionItemCard(
                    actionItem = item,
                    onToggleCompleted = { onToggleAction(item.id) }
                )
            }
        }
    }
}

@Composable
fun TranscriptTabContent(
    segments: List<TranscriptSegment>,
    fullTranscript: String,
    onSeekTo: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (segments.isNotEmpty()) {
            segments.forEach { segment ->
                val isAgency = segment.speaker.contains("Agency", ignoreCase = true) || segment.speaker.contains("Agent", ignoreCase = true)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSeekTo(segment.timestampMs) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isAgency) AgencyNavyCard else AgencyNavySurface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isAgency) AgencyPrimary.copy(alpha = 0.4f) else AgencyCyan.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = segment.speaker,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAgency) AgencyPrimaryLight else AgencyCyan
                            )
                            Text(
                                text = "▶ ${formatDuration(segment.timestampMs)}",
                                fontSize = 11.sp,
                                color = AgencyTextMuted,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Text(
                            text = segment.text,
                            fontSize = 13.sp,
                            color = AgencyTextPrimary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        } else if (fullTranscript.isNotBlank()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = AgencyNavyCard
            ) {
                Text(
                    text = fullTranscript,
                    fontSize = 13.sp,
                    color = AgencyTextPrimary,
                    modifier = Modifier.padding(14.dp),
                    lineHeight = 19.sp
                )
            }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = AgencyNavyCard
            ) {
                Text(
                    text = "Transcript is generating or unavailable for this recording.",
                    fontSize = 13.sp,
                    color = AgencyTextSecondary,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun EmailDraftTabContent(
    emailDraft: String,
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = AgencyNavyCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, AgencyBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Generated Follow-up Draft",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AgencyTextPrimary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = onCopy,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = AgencyPrimary.copy(alpha = 0.2f),
                            contentColor = AgencyPrimaryLight
                        )
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onShare,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AgencyPrimary)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Send", fontSize = 12.sp)
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = AgencyNavyDark,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = emailDraft.ifBlank { "Follow-up email draft will appear here once synthesized." },
                    fontSize = 13.sp,
                    color = AgencyTextPrimary,
                    modifier = Modifier.padding(12.dp),
                    lineHeight = 19.sp
                )
            }
        }
    }
}

@Composable
fun NotesAndBookmarksTabContent(
    notes: String,
    bookmarks: List<com.example.data.model.CallBookmark>,
    onSeekTo: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (bookmarks.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = AgencyNavyCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, AgencyBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Time-Coded Markers (${bookmarks.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = AgencyTextPrimary
                    )

                    bookmarks.forEach { bm ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSeekTo(bm.timestampMs) }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = bm.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AgencyTextPrimary
                                )
                                if (bm.note.isNotBlank()) {
                                    Text(text = bm.note, fontSize = 11.5.sp, color = AgencyTextSecondary)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = AgencyAmber.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "▶ ${formatDuration(bm.timestampMs)}",
                                    fontSize = 11.sp,
                                    color = AgencyAmber,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = AgencyNavyCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, AgencyBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Raw In-Call Notes",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AgencyTextPrimary
                )
                Text(
                    text = notes.ifBlank { "No manual in-call notes entered." },
                    fontSize = 13.sp,
                    color = AgencyTextSecondary,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
