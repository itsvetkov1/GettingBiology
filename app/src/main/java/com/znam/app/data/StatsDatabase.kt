package com.znam.app.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        QuizSession::class,
        UserProfile::class,
        Achievement::class,
        QuestionPerformance::class
    ],
    version = 4,
    exportSchema = true
)
abstract class StatsDatabase : RoomDatabase() {
    abstract fun statsDao(): StatsDao
    abstract fun gamificationDao(): GamificationDao
    abstract fun questionPerformanceDao(): QuestionPerformanceDao

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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `question_performance` (
                        `quizType` TEXT NOT NULL,
                        `questionId` INTEGER NOT NULL,
                        `timesAnswered` INTEGER NOT NULL DEFAULT 0,
                        `timesCorrect` INTEGER NOT NULL DEFAULT 0,
                        `consecutiveCorrect` INTEGER NOT NULL DEFAULT 0,
                        `consecutiveWrong` INTEGER NOT NULL DEFAULT 0,
                        `lastAnsweredAt` INTEGER NOT NULL DEFAULT 0,
                        `nextReviewAt` INTEGER NOT NULL DEFAULT 0,
                        `difficultyScore` REAL NOT NULL DEFAULT 0.5,
                        PRIMARY KEY(`quizType`, `questionId`)
                    )
                """.trimIndent())
            }
        }


        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `lastDailyChallengeDateEpochDay` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `dailyChallengeQuizType` TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
