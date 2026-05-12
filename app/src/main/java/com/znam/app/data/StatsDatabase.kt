package com.znam.app.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [QuizSession::class, UserProfile::class, Achievement::class],
    version = 2,
    exportSchema = false
)
abstract class StatsDatabase : RoomDatabase() {
    abstract fun statsDao(): StatsDao
    abstract fun gamificationDao(): GamificationDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `user_profile` (
                        `userId` INTEGER NOT NULL PRIMARY KEY,
                        `totalXp` INTEGER NOT NULL DEFAULT 0,
                        `level` INTEGER NOT NULL DEFAULT 1,
                        `currentStreak` INTEGER NOT NULL DEFAULT 0,
                        `longestStreak` INTEGER NOT NULL DEFAULT 0,
                        `lastQuizDateEpochDay` INTEGER NOT NULL DEFAULT 0,
                        `quizzesCompletedToday` INTEGER NOT NULL DEFAULT 0,
                        `perfectScoreCount` INTEGER NOT NULL DEFAULT 0,
                        `totalQuizzesCompleted` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `achievements` (
                        `achievementId` TEXT NOT NULL PRIMARY KEY,
                        `unlockedAt` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }
    }
}
