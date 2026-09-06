package com.example.data.repository

import android.content.Context
import com.example.data.db.AppDatabase
import com.example.data.db.CallDao
import com.example.data.db.ClientDao
import com.example.data.model.*
import com.example.data.network.GeminiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class CallRepository(
    private val callDao: CallDao,
    private val clientDao: ClientDao,
    private val geminiService: GeminiService = GeminiService()
) {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val stringListAdapter = moshi.adapter<List<String>>(
        Types.newParameterizedType(List::class.java, String::class.java)
    )

    private val actionItemListAdapter = moshi.adapter<List<ActionItem>>(
        Types.newParameterizedType(List::class.java, ActionItem::class.java)
    )

    private val bookmarkListAdapter = moshi.adapter<List<CallBookmark>>(
        Types.newParameterizedType(List::class.java, CallBookmark::class.java)
    )

    private val segmentListAdapter = moshi.adapter<List<TranscriptSegment>>(
        Types.newParameterizedType(List::class.java, TranscriptSegment::class.java)
    )

    private val floatListAdapter = moshi.adapter<List<Float>>(
        Types.newParameterizedType(List::class.java, java.lang.Float::class.java)
    )

    val allRecordings: Flow<List<CallRecording>> = callDao.getAllRecordings()
    val allClients: Flow<List<AgencyClient>> = clientDao.getAllClients()
    val totalRecordingsCount: Flow<Int> = callDao.getRecordingsCount()
    val totalDurationMs: Flow<Long?> = callDao.getTotalDurationMs()

    fun observeRecording(id: Long): Flow<CallRecording?> = callDao.observeRecordingById(id)

    suspend fun getRecordingById(id: Long): CallRecording? = callDao.getRecordingById(id)

    fun searchRecordings(query: String): Flow<List<CallRecording>> = callDao.searchRecordings(query)

    fun getStarredRecordings(): Flow<List<CallRecording>> = callDao.getStarredRecordings()

    suspend fun saveRecording(recording: CallRecording): Long {
        val id = callDao.insertRecording(recording)
        if (recording.clientCompany.isNotBlank() || recording.clientName.isNotBlank()) {
            clientDao.incrementCallCount(
                company = recording.clientCompany,
                name = recording.clientName,
                timestamp = recording.timestamp
            )
        }
        return id
    }

    suspend fun updateRecording(recording: CallRecording) = callDao.updateRecording(recording)

    suspend fun deleteRecording(recording: CallRecording, context: Context? = null) {
        if (recording.audioFilePath.isNotBlank()) {
            try {
                val file = File(recording.audioFilePath)
                if (file.exists()) file.delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        callDao.deleteRecording(recording)
    }

    suspend fun toggleStarred(id: Long, current: Boolean) {
        callDao.updateStarred(id, !current)
    }

    suspend fun addNewClient(client: AgencyClient): Long = clientDao.insertClient(client)

    suspend fun updateClient(client: AgencyClient) = clientDao.updateClient(client)

    suspend fun deleteClient(client: AgencyClient) = clientDao.deleteClient(client)

    // Helper conversions
    fun parseActionItems(json: String): List<ActionItem> {
        return try {
            if (json.isBlank() || json == "[]") emptyList()
            else actionItemListAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun serializeActionItems(items: List<ActionItem>): String {
        return actionItemListAdapter.toJson(items)
    }

    fun parseBookmarks(json: String): List<CallBookmark> {
        return try {
            if (json.isBlank() || json == "[]") emptyList()
            else bookmarkListAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun serializeBookmarks(bookmarks: List<CallBookmark>): String {
        return bookmarkListAdapter.toJson(bookmarks)
    }

    fun parseKeyTakeaways(json: String): List<String> {
        return try {
            if (json.isBlank() || json == "[]") emptyList()
            else stringListAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun serializeKeyTakeaways(items: List<String>): String {
        return stringListAdapter.toJson(items)
    }

    fun parseTranscriptSegments(json: String): List<TranscriptSegment> {
        return try {
            if (json.isBlank() || json == "[]") emptyList()
            else segmentListAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun serializeTranscriptSegments(segments: List<TranscriptSegment>): String {
        return segmentListAdapter.toJson(segments)
    }

    fun parseWaveform(json: String): List<Float> {
        return try {
            if (json.isBlank() || json == "[]") emptyList()
            else floatListAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun serializeWaveform(waveform: List<Float>): String {
        return floatListAdapter.toJson(waveform)
    }

    suspend fun triggerAIAnalysis(recordingId: Long): CallRecording? = withContext(Dispatchers.IO) {
        val recording = callDao.getRecordingById(recordingId) ?: return@withContext null
        
        // Mark as analyzing
        callDao.updateRecording(recording.copy(isAnalyzing = true))

        val bookmarks = parseBookmarks(recording.bookmarksJson)
        val result = geminiService.analyzeCallRecording(
            recording = recording,
            rawNotes = recording.notes,
            bookmarks = bookmarks
        )

        val updated = recording.copy(
            aiSummary = result.summary,
            transcript = result.transcript,
            transcriptSegmentsJson = serializeTranscriptSegments(result.transcriptSegments),
            keyTakeawaysJson = serializeKeyTakeaways(result.keyTakeaways),
            actionItemsJson = serializeActionItems(result.actionItems),
            clientSentiment = result.sentiment,
            dealIntentScore = result.dealIntentScore,
            dealSizeEstimate = result.dealSizeEstimate,
            keyObjections = result.keyObjections,
            followUpEmailDraft = result.followUpEmailDraft,
            isTranscribed = true,
            isAnalyzing = false
        )

        callDao.updateRecording(updated)
        return@withContext updated
    }

    suspend fun toggleActionItemStatus(recordingId: Long, actionId: String) = withContext(Dispatchers.IO) {
        val recording = callDao.getRecordingById(recordingId) ?: return@withContext
        val items = parseActionItems(recording.actionItemsJson).map { item ->
            if (item.id == actionId) item.copy(isCompleted = !item.isCompleted) else item
        }
        callDao.updateRecording(recording.copy(actionItemsJson = serializeActionItems(items)))
    }
}
