package com.domain.app.core.storage

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import com.domain.app.core.storage.dao.DataPointDao
import com.domain.app.core.storage.dao.PluginStateDao
import com.domain.app.core.storage.entity.DataPointEntity
import com.domain.app.core.storage.entity.PluginStateEntity
import com.domain.app.core.storage.converter.Converters

@Database(
    entities = [
        DataPointEntity::class,
        PluginStateEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun dataPointDao(): DataPointDao
    abstract fun pluginStateDao(): PluginStateDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getInstance(context: Context, passphrase: ByteArray): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val factory = SupportFactory(passphrase)
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .openHelperFactory(factory)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
        
        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}
