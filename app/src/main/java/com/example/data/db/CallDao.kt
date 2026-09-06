package com.example.data.db

import androidx.room.*
import com.example.data.model.CallRecording
import kotlinx.coroutines.flow.Flow

@Dao
interface CallDao {
    @Query("SELECT * FROM call_recordings ORDER BY timestamp DESC")
    fun getAllRecordings(): Flow<List<CallRecording>>

    @Query("SELECT * FROM call_recordings WHERE id = :id")
    suspend fun getRecordingById(id: Long): CallRecording?

    @Query("SELECT * FROM call_recordings WHERE id = :id")
    fun observeRecordingById(id: Long): Flow<CallRecording?>

    @Query("SELECT * FROM call_recordings WHERE isStarred = 1 ORDER BY timestamp DESC")
    fun getStarredRecordings(): Flow<List<CallRecording>>

    @Query("SELECT * FROM call_recordings WHERE clientName LIKE '%' || :query || '%' OR clientCompany LIKE '%' || :query || '%' OR title LIKE '%' || :query || '%' OR transcript LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchRecordings(query: String): Flow<List<CallRecording>>

    @Query("SELECT * FROM call_recordings WHERE clientCompany = :company ORDER BY timestamp DESC")
    fun getRecordingsByCompany(company: String): Flow<List<CallRecording>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecording(recording: CallRecording): Long

    @Update
    suspend fun updateRecording(recording: CallRecording)

    @Delete
    suspend fun deleteRecording(recording: CallRecording)

    @Query("DELETE FROM call_recordings WHERE id = :id")
    suspend fun deleteRecordingById(id: Long)

    @Query("UPDATE call_recordings SET isStarred = :isStarred WHERE id = :id")
    suspend fun updateStarred(id: Long, isStarred: Boolean)

    @Query("SELECT COUNT(*) FROM call_recordings")
    fun getRecordingsCount(): Flow<Int>

    @Query("SELECT SUM(durationMs) FROM call_recordings")
    fun getTotalDurationMs(): Flow<Long?>
}
