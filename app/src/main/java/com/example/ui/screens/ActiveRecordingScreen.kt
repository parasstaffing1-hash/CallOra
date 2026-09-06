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
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CallBookmark
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveRecordingScreen(
    viewModel: MainViewModel,
    onFinishRecording: (Long) -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    val isPaused by viewModel.isRecordingPaused.collectAsStateWithLifecycle()
    val elapsedTimeMs by viewModel.recordingElapsedTimeMs.collectAsStateWithLifecycle()
    val decibels by viewModel.recordingDecibels.collectAsStateWithLifecycle()
    val amplitudes by viewModel.liveWaveformAmplitudes.collectAsStateWithLifecycle()
    val bookmarks by viewModel.liveRecordingBookmarks.collectAsStateWithLifecycle()

    val clientName by viewModel.activeClientName.collectAsStateWithLifecycle()
    val clientCompany by viewModel.activeClientCompany.collectAsStateWithLifecycle()
    val clientPhone by viewModel.activeClientPhone.collectAsStateWithLifecycle()
    val callType by viewModel.activeCallType.collectAsStateWithLifecycle()
    val callDirection by viewModel.activeCallDirection.collectAsStateWithLifecycle()
    val platform by viewModel.activeCallPlatform.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    // Callora Live Intelligence States
    val coachingMetrics by viewModel.liveCoachingMetrics.collectAsStateWithLifecycle()
    val activeBattlecard by viewModel.activeBattlecard.collectAsStateWithLifecycle()
    val liveSnippets by viewModel.liveTranscriptSnippets.collectAsStateWithLifecycle()

    val isWhatsApp = platform.equals("WhatsApp", ignoreCase = true)

    var liveNotes by remember { mutableStateOf(viewModel.activeLiveNotes.value) }

    var showAddCustomBookmarkDialog by remember { mutableStateOf(false) }
    var showDiscardConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isWhatsApp) "Live WhatsApp Call Recording" else "Live Agency Call Recording",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = AgencyTextPrimary
                        )
                        Text(
                            text = if (isPaused) "Recording Paused"
                            else if (isWhatsApp) "🟢 VoIP Engine Active (Hardware AEC)"
                            else "Microphone Recording Active",
                            fontSize = 11.sp,
                            color = if (isPaused) AgencyAmber else AgencyEmerald
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { showDiscardConfirmDialog = true }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = AgencyTextSecondary)
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isWhatsApp) Color(0xFF25D366).copy(alpha = 0.2f) else AgencyPrimary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (isWhatsApp) "WhatsApp VoIP" else "AI Ready",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isWhatsApp) Color(0xFF25D366) else AgencyPrimaryLight,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AgencyNavySurface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(AgencyNavyDark)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // WhatsApp VoIP Banner & Quick Return Button
                if (isWhatsApp) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF075E54).copy(alpha = 0.25f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF25D366).copy(alpha = 0.4f))
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
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Chat,
                                            contentDescription = null,
                                            tint = Color(0xFF25D366),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "WhatsApp Call Recording Active",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }

                                    Button(
                                        onClick = { viewModel.launchWhatsApp(context, clientPhone) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF25D366),
                                            contentColor = Color.Black
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("Open WhatsApp ↗", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Text(
                                    text = "Recording via hardware VoIP audio channel with Acoustic Echo Cancellation. Keep speakerphone on or headset connected for maximum vocal clarity.",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.85f),
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }

                // Active Battlecard Prompt (if objection detected or tapped)
                if (activeBattlecard != null) {
                    item {
                        ActiveBattlecardPrompt(
                            battlecard = activeBattlecard!!,
                            onDismiss = { viewModel.dismissBattlecard() }
                        )
                    }
                }

                // Real-Time Talk-to-Listen Ratio & Speech Pacing HUD
                item {
                    TalkToListenRatioCard(metrics = coachingMetrics)
                }
                // Client Info Card
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = AgencyNavyCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AgencyBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            ClientAvatar(
                                name = clientName,
                                company = clientCompany,
                                size = 48
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = clientCompany.ifBlank { "Client Account" },
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AgencyTextPrimary
                                )
                                Text(
                                    text = "$clientName ${if (clientPhone.isNotBlank()) "• $clientPhone" else ""}",
                                    fontSize = 12.sp,
                                    color = AgencyTextSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    CallTypeChip(callType = callType)
                                    CallDirectionIcon(direction = callDirection)
                                }
                            }
                        }
                    }
                }

                // Studio Recording Live HUD
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = AgencyNavySurface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            if (isPaused) AgencyAmber.copy(alpha = 0.5f) else AgencyPrimary.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Big Timer
                            Text(
                                text = formatDuration(elapsedTimeMs),
                                fontSize = 48.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isPaused) AgencyAmber else Color.White,
                                letterSpacing = 2.sp
                            )

                            // Live Waveform Visualizer
                            LiveRecordingWaveform(
                                amplitudes = amplitudes,
                                decibels = decibels,
                                isPaused = isPaused,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Quick In-Call Bookmark Tagger
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "1-Tap In-Call Bookmarks",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = AgencyTextPrimary
                            )
                            Text(
                                text = "Flags key moments for AI recap",
                                fontSize = 11.sp,
                                color = AgencyTextSecondary
                            )
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val presetTags = listOf(
                                Triple("📌 Action Item", "Action Item", "Deliverable agreed upon"),
                                Triple("💡 Requirement", "Key Requirement", "Core feature or request"),
                                Triple("💰 Pricing", "Pricing", "Budget or contract discussed"),
                                Triple("⚠️ Objection", "Objection", "Client concern or risk"),
                                Triple("⭐ Highlight", "Highlight", "Crucial agreement or win")
                            )

                            items(presetTags) { (label, category, defaultNote) ->
                                Surface(
                                    modifier = Modifier.clickable {
                                        viewModel.addLiveBookmark(
                                            category = category,
                                            title = label,
                                            note = defaultNote
                                        )
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = AgencyNavyCard,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AgencyBorder)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = AgencyTextPrimary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Tagged Moments List
                if (bookmarks.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Tagged Moments (${bookmarks.size}):",
                                fontSize = 12.sp,
                                color = AgencyTextSecondary
                            )
                            bookmarks.takeLast(4).reversed().forEach { bookmark ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = AgencyNavyCard.copy(alpha = 0.7f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${bookmark.title} • ${bookmark.note}",
                                            fontSize = 12.sp,
                                            color = AgencyTextPrimary
                                        )
                                        Text(
                                            text = formatDuration(bookmark.timestampMs),
                                            fontSize = 11.sp,
                                            color = AgencyCyan,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Live Streaming Real-Time Transcription
                item {
                    LiveStreamingTranscriptSection(
                        snippets = liveSnippets,
                        onSimulateObjection = { text: String, tag: String ->
                            viewModel.simulateLiveClientTurn(text, tag)
                        }
                    )
                }

                // Live In-Call Notes Field
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Live Call Notes (Used by AI Engine):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AgencyTextPrimary
                        )
                        OutlinedTextField(
                            value = liveNotes,
                            onValueChange = {
                                liveNotes = it
                                viewModel.activeLiveNotes.value = it
                            },
                            placeholder = { Text("Jot down quick client requests, pricing numbers, or agreed deadlines during the call...", fontSize = 12.sp, color = AgencyTextMuted) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(95.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = AgencyNavyCard,
                                unfocusedContainerColor = AgencyNavyCard,
                                focusedBorderColor = AgencyPrimary,
                                unfocusedBorderColor = AgencyBorder
                            )
                        )
                    }
                }
            }

            // Bottom Floating Controls
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = AgencyNavySurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, AgencyBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Discard
                    OutlinedButton(
                        onClick = { showDiscardConfirmDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AgencyRose),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AgencyRose.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Discard", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Discard")
                    }

                    // Pause / Resume
                    FilledTonalIconButton(
                        onClick = {
                            if (isPaused) viewModel.resumeRecording() else viewModel.pauseRecording()
                        },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (isPaused) AgencyEmerald.copy(alpha = 0.2f) else AgencyAmber.copy(alpha = 0.2f),
                            contentColor = if (isPaused) AgencyEmerald else AgencyAmber
                        ),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause,
                            contentDescription = if (isPaused) "Resume" else "Pause",
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // Finish & AI Analyze
                    Button(
                        onClick = {
                            viewModel.finishAndSaveRecording { savedId ->
                                onFinishRecording(savedId)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AgencyPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("finish_recording_button")
                    ) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = "Finish", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Finish & Analyze", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showDiscardConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirmDialog = false },
            title = { Text("Discard Recording?", color = AgencyTextPrimary) },
            text = { Text("Are you sure you want to discard this call recording? Audio and notes will not be saved.", color = AgencyTextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        showDiscardConfirmDialog = false
                        viewModel.discardRecording()
                        onDiscard()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AgencyRose)
                ) {
                    Text("Discard Call")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirmDialog = false }) {
                    Text("Keep Recording", color = AgencyTextPrimary)
                }
            },
            containerColor = AgencyNavySurface
        )
    }
}
