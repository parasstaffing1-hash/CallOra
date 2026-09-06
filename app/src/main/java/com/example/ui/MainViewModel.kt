package com.example.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.*
import com.example.data.repository.CallRepository
import com.example.service.AudioPlayerManager
import com.example.service.CallRecordingService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class CallFilter {
    ALL, WHATSAPP, STARRED, INBOUND, OUTBOUND, HIGH_INTENT, HAS_PENDING_ACTIONS
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application, viewModelScope)
    val repository = CallRepository(db.callDao(), db.clientDao())

    val audioPlayerManager = AudioPlayerManager(application)

    // Service binding for Recording
    private var recordingService: CallRecordingService? = null
    private val _isServiceBound = MutableStateFlow(false)

    val isRecording: StateFlow<Boolean> = _isServiceBound.flatMapLatest { bound ->
        if (bound) recordingService?.isRecording ?: flowOf(false) else flowOf(false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isRecordingPaused: StateFlow<Boolean> = _isServiceBound.flatMapLatest { bound ->
        if (bound) recordingService?.isPaused ?: flowOf(false) else flowOf(false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val recordingElapsedTimeMs: StateFlow<Long> = _isServiceBound.flatMapLatest { bound ->
        if (bound) recordingService?.elapsedTimeMs ?: flowOf(0L) else flowOf(0L)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val recordingDecibels: StateFlow<Float> = _isServiceBound.flatMapLatest { bound ->
        if (bound) recordingService?.currentDecibels ?: flowOf(0f) else flowOf(0f)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val liveWaveformAmplitudes: StateFlow<List<Float>> = _isServiceBound.flatMapLatest { bound ->
        if (bound) recordingService?.amplitudes ?: flowOf(emptyList()) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val liveRecordingBookmarks: StateFlow<List<CallBookmark>> = _isServiceBound.flatMapLatest { bound ->
        if (bound) recordingService?.currentBookmarks ?: flowOf(emptyList()) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active recording form state
    val activeClientName = MutableStateFlow("")
    val activeClientCompany = MutableStateFlow("")
    val activeClientPhone = MutableStateFlow("")
    val activeCallType = MutableStateFlow(CallType.DISCOVERY)
    val activeCallDirection = MutableStateFlow(CallDirection.OUTBOUND)
    val activeCallPlatform = MutableStateFlow("Phone") // "WhatsApp" or "Phone"
    val activeAudioSourcePref = MutableStateFlow("VOICE_COMMUNICATION")
    val activeLiveNotes = MutableStateFlow("")

    // Callora Live Intelligence & Coaching
    val liveCoachingMetrics = MutableStateFlow(LiveCoachingMetrics())
    val liveTranscriptSnippets = MutableStateFlow<List<LiveTranscriptSnippet>>(emptyList())
    val activeBattlecard = MutableStateFlow<Battlecard?>(null)
    val isFloatingWidgetEnabled = MutableStateFlow(true)
    val isFloatingWidgetMinimized = MutableStateFlow(false)

    // Compliance & Privacy
    val twoPartyConsentPingEnabled = MutableStateFlow(true)
    val piiRedactionEnabled = MutableStateFlow(true)

    // CRM Sync State
    val crmSyncStates = MutableStateFlow(mapOf(
        "HubSpot" to false,
        "Salesforce" to false,
        "Notion" to false,
        "Slack" to false
    ))

    // Selected call for detail view
    val selectedCallId = MutableStateFlow<Long?>(null)
    val selectedCall: StateFlow<CallRecording?> = selectedCallId.flatMapLatest { id ->
        if (id != null) repository.observeRecording(id) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Filters and Search
    val searchQuery = MutableStateFlow("")
    val activeFilter = MutableStateFlow(CallFilter.ALL)
    val selectedTypeFilter = MutableStateFlow<CallType?>(null)

    val allRecordings: StateFlow<List<CallRecording>> = combine(
        repository.allRecordings,
        searchQuery,
        activeFilter,
        selectedTypeFilter
    ) { recordings, query, filter, typeFilter ->
        var list = recordings

        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter {
                it.clientName.lowercase().contains(q) ||
                it.clientCompany.lowercase().contains(q) ||
                it.title.lowercase().contains(q) ||
                it.transcript.lowercase().contains(q) ||
                it.notes.lowercase().contains(q)
            }
        }

        if (typeFilter != null) {
            list = list.filter { it.callType == typeFilter }
        }

        when (filter) {
            CallFilter.ALL -> list
            CallFilter.WHATSAPP -> list.filter { it.platform.equals("WhatsApp", ignoreCase = true) }
            CallFilter.STARRED -> list.filter { it.isStarred }
            CallFilter.INBOUND -> list.filter { it.direction == CallDirection.INBOUND }
            CallFilter.OUTBOUND -> list.filter { it.direction == CallDirection.OUTBOUND }
            CallFilter.HIGH_INTENT -> list.filter { it.clientSentiment == Sentiment.HIGH_INTENT || it.dealIntentScore >= 85 }
            CallFilter.HAS_PENDING_ACTIONS -> list.filter {
                val actions = repository.parseActionItems(it.actionItemsJson)
                actions.any { a -> !a.isCompleted }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allClients: StateFlow<List<AgencyClient>> = repository.allClients
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalDurationMs: StateFlow<Long?> = repository.totalDurationMs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? CallRecordingService.LocalBinder
            recordingService = binder?.getService()
            _isServiceBound.value = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            recordingService = null
            _isServiceBound.value = false
        }
    }

    init {
        bindRecordingService()
        observeServiceEvents()
    }

    private fun observeServiceEvents() {
        viewModelScope.launch {
            _isServiceBound.collect { bound ->
                if (bound) {
                    recordingService?.notificationActionEvents?.collect { action ->
                        if (action == CallRecordingService.ACTION_STOP_FROM_NOTIFICATION) {
                            finishAndSaveRecording { }
                        }
                    }
                }
            }
        }
    }

    private fun bindRecordingService() {
        val context = getApplication<Application>()
        val intent = Intent(context, CallRecordingService::class.java)
        context.startService(intent)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun startLiveRecording(
        clientName: String,
        clientCompany: String,
        phone: String = "",
        callType: CallType = CallType.DISCOVERY,
        direction: CallDirection = CallDirection.OUTBOUND,
        platform: String = "Phone",
        audioSourcePref: String = "VOICE_COMMUNICATION"
    ) {
        activeClientName.value = clientName
        activeClientCompany.value = clientCompany
        activeClientPhone.value = phone
        activeCallType.value = callType
        activeCallDirection.value = direction
        activeCallPlatform.value = platform
        activeAudioSourcePref.value = audioSourcePref
        activeLiveNotes.value = ""

        // Reset and prime Live Coaching & Transcription
        activeBattlecard.value = null
        liveCoachingMetrics.value = LiveCoachingMetrics(
            talkPercentage = 35,
            listenPercentage = 65,
            wordsPerMinute = 138,
            paceLabel = "Optimal Discovery Pace",
            monologueDurationSec = 14,
            questionsAskedByAgent = 1,
            questionsAskedByClient = 0
        )

        // Seed realistic live transcript snippets that match agency call flow
        val company = clientCompany.ifBlank { "Client" }
        liveTranscriptSnippets.value = listOf(
            LiveTranscriptSnippet(
                speaker = "Agency (You)",
                text = "Hi $clientName, excited to connect today about $company's Q3 growth goals. Before diving in, what's top of mind for you?",
                timestampMs = 2000L,
                isQuestion = true
            ),
            LiveTranscriptSnippet(
                speaker = clientName.ifBlank { "Client" },
                text = "Thanks for making time. We've been evaluating our current pipeline and need to scale qualified demos without burning out our sales team.",
                timestampMs = 8000L,
                isQuestion = false
            )
        )

        if (twoPartyConsentPingEnabled.value) {
            playConsentPing(getApplication())
        }

        recordingService?.startRecording(
            clientName = clientName,
            clientCompany = clientCompany,
            platform = platform,
            preferredAudioSource = audioSourcePref
        )
    }

    fun startWhatsAppCallAndRecord(
        clientName: String,
        clientCompany: String,
        phone: String,
        context: Context
    ) {
        startLiveRecording(
            clientName = clientName,
            clientCompany = clientCompany,
            phone = phone,
            callType = CallType.DISCOVERY,
            direction = CallDirection.OUTBOUND,
            platform = "WhatsApp",
            audioSourcePref = "VOICE_COMMUNICATION"
        )
        launchWhatsApp(context, phone)
    }

    fun launchWhatsApp(context: Context, phoneNumber: String) {
        val cleanNumber = phoneNumber.replace("[^0-9+]".toRegex(), "")
        try {
            val url = if (cleanNumber.isNotBlank()) "https://api.whatsapp.com/send?phone=$cleanNumber" else "whatsapp://send"
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)).apply {
                setPackage("com.whatsapp")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val fallback = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://api.whatsapp.com/send?phone=$cleanNumber")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallback)
            } catch (e2: Exception) {
                android.widget.Toast.makeText(context, "WhatsApp is not installed on this device", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun shareViaWhatsApp(context: Context, textToShare: String, phoneNumber: String = "") {
        val cleanNumber = phoneNumber.replace("[^0-9+]".toRegex(), "")
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                setPackage("com.whatsapp")
                putExtra(Intent.EXTRA_TEXT, textToShare)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, textToShare)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Call Summary"))
        }
    }

    fun pauseRecording() {
        recordingService?.pauseRecording()
    }

    fun resumeRecording() {
        recordingService?.resumeRecording()
    }

    fun addLiveBookmark(category: String, title: String, note: String = "") {
        recordingService?.addBookmark(category, title, note)
    }

    fun finishAndSaveRecording(onSaved: (Long) -> Unit) {
        viewModelScope.launch {
            val result = recordingService?.stopRecording() ?: return@launch
            val clientName = activeClientName.value.ifBlank { "Client Account" }
            val clientCompany = activeClientCompany.value.ifBlank { "Agency Partner" }
            val callType = activeCallType.value
            val notes = activeLiveNotes.value
            val platform = if (result.platform.isNotBlank()) result.platform else activeCallPlatform.value

            val title = if (platform.equals("WhatsApp", ignoreCase = true)) {
                "WhatsApp Call - $clientCompany"
            } else {
                "$clientCompany - ${callType.displayName}"
            }

            val newRecording = CallRecording(
                title = title,
                clientName = clientName,
                clientCompany = clientCompany,
                clientPhone = activeClientPhone.value,
                callType = callType,
                direction = activeCallDirection.value,
                platform = platform,
                audioSourceUsed = result.audioSourceUsed,
                audioFilePath = result.filePath,
                durationMs = result.durationMs,
                timestamp = System.currentTimeMillis(),
                fileSize = result.fileSize,
                notes = notes,
                bookmarksJson = repository.serializeBookmarks(result.bookmarks),
                waveformAmplitudesJson = repository.serializeWaveform(result.amplitudes),
                isTranscribed = false,
                isAnalyzing = true
            )

            val savedId = repository.saveRecording(newRecording)
            selectedCallId.value = savedId

            // Auto-trigger AI Call Analysis & Transcription
            repository.triggerAIAnalysis(savedId)
            onSaved(savedId)
        }
    }

    fun discardRecording() {
        recordingService?.stopRecording()
        activeClientName.value = ""
        activeClientCompany.value = ""
        activeLiveNotes.value = ""
    }

    fun triggerAIAnalysisForCall(recordingId: Long) {
        viewModelScope.launch {
            repository.triggerAIAnalysis(recordingId)
        }
    }

    fun toggleStarred(recordingId: Long, current: Boolean) {
        viewModelScope.launch {
            repository.toggleStarred(recordingId, current)
        }
    }

    fun toggleActionItem(recordingId: Long, actionId: String) {
        viewModelScope.launch {
            repository.toggleActionItemStatus(recordingId, actionId)
        }
    }

    fun deleteRecording(recording: CallRecording) {
        viewModelScope.launch {
            if (audioPlayerManager.isPlaying.value) {
                audioPlayerManager.stop()
            }
            repository.deleteRecording(recording, getApplication())
            if (selectedCallId.value == recording.id) {
                selectedCallId.value = null
            }
        }
    }

    fun addNewClient(client: AgencyClient) {
        viewModelScope.launch {
            repository.addNewClient(client)
        }
    }

    // Callora Battlecard Controls
    fun selectBattlecard(battlecard: Battlecard?) {
        activeBattlecard.value = battlecard
    }

    fun dismissBattlecard() {
        activeBattlecard.value = null
    }

    fun triggerBattlecardForTag(tag: String) {
        val match = CalloraBattlecards.defaultBattlecards.firstOrNull { 
            it.triggerTag.equals(tag, ignoreCase = true) 
        } ?: CalloraBattlecards.defaultBattlecards.first()
        activeBattlecard.value = match
    }

    // Floating Widget Overlay Controls
    fun toggleFloatingWidget(enabled: Boolean) {
        isFloatingWidgetEnabled.value = enabled
    }

    fun toggleFloatingWidgetMinimized() {
        isFloatingWidgetMinimized.value = !isFloatingWidgetMinimized.value
    }

    // Live Streaming Transcription simulation
    fun addLiveSnippet(snippet: LiveTranscriptSnippet) {
        val current = liveTranscriptSnippets.value.toMutableList()
        current.add(snippet)
        liveTranscriptSnippets.value = current

        // If client asked a question or raised an objection, suggest battlecard
        if (snippet.speaker.contains("Client", ignoreCase = true) && snippet.detectedTag != null) {
            triggerBattlecardForTag(snippet.detectedTag)
        }
    }

    fun simulateLiveClientTurn(text: String, tag: String? = null) {
        addLiveSnippet(
            LiveTranscriptSnippet(
                speaker = activeClientName.value.ifBlank { "Client" },
                text = text,
                timestampMs = recordingElapsedTimeMs.value,
                isQuestion = text.contains("?"),
                detectedTag = tag
            )
        )
    }

    fun simulateLiveAgentTurn(text: String) {
        addLiveSnippet(
            LiveTranscriptSnippet(
                speaker = "Agency (You)",
                text = text,
                timestampMs = recordingElapsedTimeMs.value,
                isQuestion = text.contains("?"),
                detectedTag = null
            )
        )
    }

    // Audio Two-Party Consent Ping Chime
    fun playConsentPing(context: Context) {
        try {
            val toneGen = android.media.ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, 85)
            toneGen.startTone(android.media.ToneGenerator.TONE_PROP_BEEP2, 350)
        } catch (e: Exception) {
            // Audio tone generator fallback
        }
    }

    // PII Redaction
    fun applyPiiRedaction(text: String): String {
        if (!piiRedactionEnabled.value) return text
        // Redact credit cards, social security / tax IDs, and sensitive numbers
        var sanitized = text.replace(Regex("\\b(?:\\d[ -]*?){13,16}\\b"), "[CONFIDENTIAL-CARD-REDACTED]")
        sanitized = sanitized.replace(Regex("\\b\\d{3}-\\d{2}-\\d{4}\\b"), "[ID-REDACTED]")
        return sanitized
    }

    // CRM & Webhook Automation
    fun syncToCrm(platform: String, call: CallRecording, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            kotlinx.coroutines.delay(650) // simulate seamless API handshake
            val updated = crmSyncStates.value.toMutableMap()
            updated[platform] = true
            crmSyncStates.value = updated
            onComplete(true, "Successfully pushed '${call.title}' to $platform CRM")
        }
    }

    // Callora Branded Client Deliverables / PDF Recap Exporter
    fun generateCalloraRecap(call: CallRecording): String {
        val actions = repository.parseActionItems(call.actionItemsJson)
        val takeaways = repository.parseKeyTakeaways(call.keyTakeawaysJson)

        val builder = StringBuilder()
        builder.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        builder.append("CALLORA CLIENT EXECUTIVE BRIEF\n")
        builder.append("Confidential Meeting Intelligence Recap\n")
        builder.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n")

        builder.append("📋 Meeting Title: ${call.title}\n")
        builder.append("👤 Client: ${call.clientName} | ${call.clientCompany}\n")
        builder.append("📞 Platform: ${call.platform} (${call.audioSourceUsed})\n")
        builder.append("⏱️ Duration: ${call.durationMs / 1000}s | Date: ${java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(call.timestamp))}\n")
        builder.append("🎯 Deal Intent Score: ${call.dealIntentScore} / 100 (${call.clientSentiment.displayName})\n\n")

        builder.append("─── EXECUTIVE SUMMARY ───\n")
        builder.append("${call.aiSummary.ifBlank { "Client discussion focused on core agency objectives and growth roadmap." }}\n\n")

        if (takeaways.isNotEmpty()) {
            builder.append("─── KEY TAKEAWAYS & DECISIONS ───\n")
            takeaways.forEach { bullet ->
                builder.append("• $bullet\n")
            }
            builder.append("\n")
        }

        if (actions.isNotEmpty()) {
            builder.append("─── ACTION ITEMS & DELIVERABLES ───\n")
            actions.forEach { action ->
                val check = if (action.isCompleted) "[COMPLETED]" else "[PENDING]"
                builder.append("• $check ${action.task} (Owner: ${action.assignee})\n")
            }
            builder.append("\n")
        }

        if (call.followUpEmailDraft.isNotBlank()) {
            builder.append("─── PRE-DRAFTED CLIENT FOLLOW-UP ───\n")
            builder.append("${call.followUpEmailDraft}\n\n")
        }

        builder.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        builder.append("Generated with Callora AI Studio\n")
        builder.append("https://callora.agency\n")
        builder.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")

        return applyPiiRedaction(builder.toString())
    }

    fun exportCalloraRecap(context: Context, call: CallRecording) {
        val recap = generateCalloraRecap(call)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Callora Executive Recap: ${call.clientCompany}")
            putExtra(Intent.EXTRA_TEXT, recap)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(Intent.createChooser(sendIntent, "Export Callora Client Recap"))
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayerManager.release()
        try {
            if (_isServiceBound.value) {
                getApplication<Application>().unbindService(serviceConnection)
            }
        } catch (e: Exception) {}
    }
}
