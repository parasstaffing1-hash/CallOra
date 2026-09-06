package com.example.data.db

import androidx.room.*
import com.example.data.model.AgencyClient
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientDao {
    @Query("SELECT * FROM agency_clients ORDER BY lastCallTimestamp DESC")
    fun getAllClients(): Flow<List<AgencyClient>>

    @Query("SELECT * FROM agency_clients WHERE id = :id")
    suspend fun getClientById(id: Long): AgencyClient?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: AgencyClient): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClients(clients: List<AgencyClient>)

    @Update
    suspend fun updateClient(client: AgencyClient)

    @Delete
    suspend fun deleteClient(client: AgencyClient)

    @Query("UPDATE agency_clients SET totalCallsCount = totalCallsCount + 1, lastCallTimestamp = :timestamp WHERE company = :company OR name = :name")
    suspend fun incrementCallCount(company: String, name: String, timestamp: Long)
}
