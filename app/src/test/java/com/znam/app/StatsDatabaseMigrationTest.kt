package com.znam.app

import android.content.Context
import androidx.room.migration.Migration
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.znam.app.data.StatsDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StatsDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        StatsDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    private fun createDatabase(dbName: String, version: Int, block: SupportSQLiteDatabase.() -> Unit) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(version) {
                    override fun onCreate(db: SupportSQLiteDatabase) = db.block()
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        ).writableDatabase.close()
    }

    private fun createV1(dbName: String) = createDatabase(dbName, 1) {
        execSQL("CREATE TABLE IF NOT EXISTS `quiz_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `quizType` TEXT NOT NULL, `score` INTEGER NOT NULL, `totalQuestions` INTEGER NOT NULL, `elapsedTimeSeconds` INTEGER NOT NULL, `hintsUsed` INTEGER NOT NULL DEFAULT 0, `timestamp` INTEGER NOT NULL)")
        execSQL("INSERT INTO quiz_sessions (quizType, score, totalQuestions, elapsedTimeSeconds, hintsUsed, timestamp) VALUES ('class9.db', 7, 15, 123, 2, 1000)")
    }

    private fun createV2(dbName: String) = createDatabase(dbName, 2) {
        execSQL("CREATE TABLE IF NOT EXISTS `quiz_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `quizType` TEXT NOT NULL, `score` INTEGER NOT NULL, `totalQuestions` INTEGER NOT NULL, `elapsedTimeSeconds` INTEGER NOT NULL, `hintsUsed` INTEGER NOT NULL DEFAULT 0, `timestamp` INTEGER NOT NULL)")
        StatsDatabase.MIGRATION_1_2.migrate(this)
        execSQL("INSERT INTO quiz_sessions (quizType, score, totalQuestions, elapsedTimeSeconds, hintsUsed, timestamp) VALUES ('class10.db', 9, 15, 95, 1, 2000)")
    }

    private fun createV3(dbName: String) = createDatabase(dbName, 3) {
        execSQL("CREATE TABLE IF NOT EXISTS `quiz_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `quizType` TEXT NOT NULL, `score` INTEGER NOT NULL, `totalQuestions` INTEGER NOT NULL, `elapsedTimeSeconds` INTEGER NOT NULL, `hintsUsed` INTEGER NOT NULL DEFAULT 0, `timestamp` INTEGER NOT NULL)")
        StatsDatabase.MIGRATION_1_2.migrate(this)
        StatsDatabase.MIGRATION_2_3.migrate(this)
        execSQL("INSERT INTO quiz_sessions (quizType, score, totalQuestions, elapsedTimeSeconds, hintsUsed, timestamp) VALUES ('db_entrance_exam.db', 12, 15, 88, 0, 3000)")
    }

    private data class ExpectedSession(
        val quizType: String,
        val score: Int,
        val totalQuestions: Int,
        val elapsedTimeSeconds: Int,
        val hintsUsed: Int,
        val timestamp: Long
    )

    private fun assertMigratedSession(
        dbName: String,
        expected: ExpectedSession,
        vararg migrations: Migration
    ) {
        val db = helper.runMigrationsAndValidate(dbName, 4, true, *migrations)
        db.query("SELECT quizType, score, totalQuestions, elapsedTimeSeconds, hintsUsed, timestamp FROM quiz_sessions").use { cursor ->
            cursor.moveToFirst()
            assertEquals(expected.quizType, cursor.getString(0))
            assertEquals(expected.score, cursor.getInt(1))
            assertEquals(expected.totalQuestions, cursor.getInt(2))
            assertEquals(expected.elapsedTimeSeconds, cursor.getInt(3))
            assertEquals(expected.hintsUsed, cursor.getInt(4))
            assertEquals(expected.timestamp, cursor.getLong(5))
        }
        db.query("PRAGMA table_info(`user_profile`)").use { cursor ->
            val defaults = mutableMapOf<String, String?>()
            while (cursor.moveToNext()) defaults[cursor.getString(1)] = cursor.getString(4)
            assertEquals("0", defaults["lastDailyChallengeDateEpochDay"])
            assertEquals("''", defaults["dailyChallengeQuizType"])
        }
        db.close()
        ApplicationProvider.getApplicationContext<Context>().deleteDatabase(dbName)
    }

    @Test
    fun migration1To4_validatesSchemaAndPreservesQuizSessions() {
        val dbName = "stats_migration_1_4.db"
        ApplicationProvider.getApplicationContext<Context>().deleteDatabase(dbName)
        createV1(dbName)
        assertMigratedSession(
            dbName,
            ExpectedSession("class9.db", 7, 15, 123, 2, 1000),
            StatsDatabase.MIGRATION_1_2,
            StatsDatabase.MIGRATION_2_3,
            StatsDatabase.MIGRATION_3_4
        )
    }

    @Test
    fun migration2To4_validatesSchemaAndPreservesQuizSessions() {
        val dbName = "stats_migration_2_4.db"
        ApplicationProvider.getApplicationContext<Context>().deleteDatabase(dbName)
        createV2(dbName)
        assertMigratedSession(
            dbName,
            ExpectedSession("class10.db", 9, 15, 95, 1, 2000),
            StatsDatabase.MIGRATION_2_3,
            StatsDatabase.MIGRATION_3_4
        )
    }

    @Test
    fun migration3To4_validatesSchemaAndPreservesQuizSessions() {
        val dbName = "stats_migration_3_4.db"
        ApplicationProvider.getApplicationContext<Context>().deleteDatabase(dbName)
        createV3(dbName)
        assertMigratedSession(
            dbName,
            ExpectedSession("db_entrance_exam.db", 12, 15, 88, 0, 3000),
            StatsDatabase.MIGRATION_3_4
        )
    }
}
