package com.domain.app.core.storage.converter

import androidx.room.TypeConverter
import java.time.Instant

class Converters {
    
    @TypeConverter
    fun fromInstant(value: Instant?): Long? {
        return value?.toEpochMilli()
    }
    
    @TypeConverter
    fun toInstant(value: Long?): Instant? {
        return value?.let { Instant.ofEpochMilli(it) }
    }
}
