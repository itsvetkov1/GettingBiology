package com.znam.app.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [QuizSession::class],
    version = 1,
    exportSchema = false
)
abstract class StatsDatabase : RoomDatabase() {
    abstract fun statsDao(): StatsDao
}
