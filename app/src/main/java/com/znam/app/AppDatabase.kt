package com.znam.app

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Include UserProgress class in the entities array
@Database(entities = [Question::class, UserProgress::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun questionDao(): QuestionDao

    // Add an abstract method to get UserProgressDao
    abstract fun userProgressDao(): UserProgressDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Version 3 formalizes the existing asset schema; no table changes are required.
            }
        }

        val ALL_MIGRATIONS = arrayOf(MIGRATION_2_3)
    }
}
