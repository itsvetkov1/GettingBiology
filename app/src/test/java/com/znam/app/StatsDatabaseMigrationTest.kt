package com.znam.app

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.znam.app.data.StatsDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test

@org.junit.runner.RunWith(AndroidJUnit4::class)
class StatsDatabaseMigrationTest {
    @Suppress("unused")
    private val migrationTestHelperType = MigrationTestHelper::class

    @Test
    fun migration1To2_createsGamificationTablesWithRoomMatchingDefaults() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "stats_migration_1_2.db"
        context.deleteDatabase(dbName)
        val db = FrameworkSQLiteOpenHelperFactory().create(
            androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE IF NOT EXISTS `quiz_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `quizType` TEXT NOT NULL, `score` INTEGER NOT NULL, `totalQuestions` INTEGER NOT NULL, `elapsedTimeSeconds` INTEGER NOT NULL, `hintsUsed` INTEGER NOT NULL DEFAULT 0, `timestamp` INTEGER NOT NULL)")
                    }
                    override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        ).writableDatabase
        db.close()

        val migrated = Room.databaseBuilder(context, StatsDatabase::class.java, dbName)
            .addMigrations(StatsDatabase.MIGRATION_1_2, StatsDatabase.MIGRATION_2_3)
            .build()
        migrated.openHelper.writableDatabase.query("PRAGMA table_info(`user_profile`)").use { cursor ->
            val defaults = mutableMapOf<String, String?>()
            while (cursor.moveToNext()) defaults[cursor.getString(1)] = cursor.getString(4)
            assertEquals("0", defaults["totalXp"])
            assertEquals("1", defaults["level"])
            assertEquals("0", defaults["lastQuizDateEpochDay"])
        }
        migrated.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun migration2To3_createsQuestionPerformanceWithRoomMatchingDefaults() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "stats_migration_2_3.db"
        context.deleteDatabase(dbName)
        val db = FrameworkSQLiteOpenHelperFactory().create(
            androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(2) {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE IF NOT EXISTS `quiz_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `quizType` TEXT NOT NULL, `score` INTEGER NOT NULL, `totalQuestions` INTEGER NOT NULL, `elapsedTimeSeconds` INTEGER NOT NULL, `hintsUsed` INTEGER NOT NULL DEFAULT 0, `timestamp` INTEGER NOT NULL)")
                        StatsDatabase.MIGRATION_1_2.migrate(db)
                    }
                    override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        ).writableDatabase
        db.close()

        val migrated = Room.databaseBuilder(context, StatsDatabase::class.java, dbName)
            .addMigrations(StatsDatabase.MIGRATION_2_3)
            .build()
        migrated.openHelper.writableDatabase.query("PRAGMA table_info(`question_performance`)").use { cursor ->
            val defaults = mutableMapOf<String, String?>()
            while (cursor.moveToNext()) defaults[cursor.getString(1)] = cursor.getString(4)
            assertEquals("0", defaults["timesAnswered"])
            assertEquals("0.5", defaults["difficultyScore"])
        }
        migrated.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun migration1To3_runsBothMigrationPaths() {
        assertEquals(1, StatsDatabase.MIGRATION_1_2.startVersion)
        assertEquals(2, StatsDatabase.MIGRATION_1_2.endVersion)
        assertEquals(2, StatsDatabase.MIGRATION_2_3.startVersion)
        assertEquals(3, StatsDatabase.MIGRATION_2_3.endVersion)
    }
}
