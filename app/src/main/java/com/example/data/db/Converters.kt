package com.example.data.db

import androidx.room.TypeConverter
import com.example.data.model.CallDirection
import com.example.data.model.CallType
import com.example.data.model.Sentiment

class Converters {
    @TypeConverter
    fun fromCallType(value: CallType): String = value.name

    @TypeConverter
    fun toCallType(value: String): CallType = try {
        CallType.valueOf(value)
    } catch (e: Exception) {
        CallType.DISCOVERY
    }

    @TypeConverter
    fun fromCallDirection(value: CallDirection): String = value.name

    @TypeConverter
    fun toCallDirection(value: String): CallDirection = try {
        CallDirection.valueOf(value)
    } catch (e: Exception) {
        CallDirection.OUTBOUND
    }

    @TypeConverter
    fun fromSentiment(value: Sentiment): String = value.name

    @TypeConverter
    fun toSentiment(value: String): Sentiment = try {
        Sentiment.valueOf(value)
    } catch (e: Exception) {
        Sentiment.NEUTRAL
    }
}
