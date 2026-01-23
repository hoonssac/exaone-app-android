package com.example.exaoneagent.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.exaoneagent.data.db.dao.ThreadDao
import com.example.exaoneagent.data.db.dao.MessageDao
import com.example.exaoneagent.data.db.entity.ThreadEntity
import com.example.exaoneagent.data.db.entity.MessageEntity

@Database(
    entities = [ThreadEntity::class, MessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun threadDao(): ThreadDao
    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "exaone_agent.db"
                ).build().also { instance = it }
            }
        }
    }
}
