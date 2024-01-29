package com.example.gettingbiology

import androidx.room.Database
import androidx.room.RoomDatabase

// Include UserProgress class in the entities array
@Database(entities = [Question::class, UserProgress::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun questionDao(): QuestionDao

    // Add an abstract method to get UserProgressDao
    abstract fun userProgressDao(): UserProgressDao
}
