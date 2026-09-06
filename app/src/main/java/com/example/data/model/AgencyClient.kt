package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "agency_clients")
@JsonClass(generateAdapter = true)
data class AgencyClient(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val company: String,
    val phone: String,
    val email: String,
    val stage: String = "Active Client", // Lead, Proposal Sent, In Negotiation, Active Client, Retainer
    val monthlyValue: String = "$5,000/mo",
    val primaryContact: String = "Direct",
    val avatarColorHex: String = "#6366F1",
    val totalCallsCount: Int = 0,
    val lastCallTimestamp: Long = System.currentTimeMillis()
)
