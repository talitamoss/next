package com.domain.app.core.storage

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.domain.app.core.storage.converter.Converters
import com.domain.app.core.storage.dao.DataPointDao
import com.domain.app.core.storage.dao.PluginStateDao
import com.domain.app.core.storage.entity.DataPointEntity
import com.domain.app.core.storage.entity.PluginStateEntity

/**
 * Main application database
 * 
 * File location: app/src/main/java/com/domain/app/core/storage/AppDatabase.kt
 */
@Database(
    entities = [
        DataPointEntity::class,
        PluginStateEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dataPointDao(): DataPointDao
    abstract fun pluginStateDao(): PluginStateDao
    
    companion object {
        private const val DATABASE_NAME = "app_database"
        
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }
        
        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
            .fallbackToDestructiveMigration() // For development only
            .build()
        }
    }
}
