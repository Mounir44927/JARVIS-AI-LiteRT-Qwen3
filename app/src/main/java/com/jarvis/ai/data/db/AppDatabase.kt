package com.jarvis.ai.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [MasterProfile::class, Fact::class, SearchMemory::class, Message::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao
}
