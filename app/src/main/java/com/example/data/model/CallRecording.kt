package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

enum class CallType(val displayName: String) {
    DISCOVERY("Discovery Call"),
    CLIENT_PITCH("Pitch / Proposal"),
    STATUS_UPDATE("Status Update"),
    NEGOTIATION("Contract & Pricing"),
    CLIENT_SUPPORT("Account Support"),
    INTERNAL_STRATEGY("Strategy & Review"),
    CONSULTING("Consulting Session")
}

enum class CallDirection(val displayName: String) {
    INBOUND("Inbound"),
    OUTBOUND("Outbound"),
    MEETING("Agency Meeting")
}

enum class Sentiment(val displayName: String, val score: Int) {
    HIGH_INTENT("High Intent", 95),
    POSITIVE("Positive", 80),
    NEUTRAL("Neutral", 50),
    HESITANT("Hesitant", 35),
    AT_RISK("At Risk", 20)
}

@JsonClass(generateAdapter = true)
data class ActionItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val task: String,
    val assignee: String = "Agency Rep",
    val dueDate: String = "",
    val isCompleted: Boolean = false,
    val priority: String = "Medium" // High, Medium, Low
)

@JsonClass(generateAdapter = true)
data class CallBookmark(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestampMs: Long,
    val title: String,
    val category: String = "General", // "Action Item", "Key Requirement", "Pricing", "Objection", "Highlight"
    val note: String = ""
)

@JsonClass(generateAdapter = true)
data class TranscriptSegment(
    val speaker: String, // "Agency Agent" or "Client"
    val timestampMs: Long,
    val text: String
)

@Entity(tableName = "call_recordings")
@JsonClass(generateAdapter = true)
data class CallRecording(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val clientName: String,
    val clientCompany: String,
    val clientPhone: String = "",
    val clientEmail: String = "",
    val callType: CallType = CallType.DISCOVERY,
    val direction: CallDirection = CallDirection.OUTBOUND,
    val platform: String = "Phone", // "WhatsApp" or "Phone"
    val audioSourceUsed: String = "VOICE_COMMUNICATION (VoIP)",
    val audioFilePath: String = "",
    val durationMs: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val fileSize: Long = 0L,
    val isStarred: Boolean = false,
    val notes: String = "",
    
    // AI Generated Insights
    val aiSummary: String = "",
    val transcript: String = "",
    val transcriptSegmentsJson: String = "[]",
    val keyTakeawaysJson: String = "[]",
    val actionItemsJson: String = "[]",
    val bookmarksJson: String = "[]",
    val clientSentiment: Sentiment = Sentiment.NEUTRAL,
    val dealIntentScore: Int = 70, // 0 - 100
    val dealSizeEstimate: String = "",
    val keyObjections: String = "",
    val followUpEmailDraft: String = "",
    val waveformAmplitudesJson: String = "[]",
    val isTranscribed: Boolean = false,
    val isAnalyzing: Boolean = false
)
