package com.znam.app

import android.content.Context
import androidx.room.Room
import com.znam.app.data.StatsDatabase

class DatabaseProvider(private val context: Context?) {

    @Volatile
    private var statsDatabase: StatsDatabase? = null

    fun databaseNameForQuizType(quizType: String): String {
        return when (quizType) {
            "class8.db" -> "class8.db"
            "class9.db" -> "class9.db"
            "class10.db" -> "class10.db"
            "db_entrance_exam.db" -> "db_entrance_exam.db"
            else -> "dbquestions.db"
        }
    }

    fun createDatabase(quizType: String): AppDatabase {
        val appContext = requireNotNull(context) { "Context is required to create AppDatabase" }.applicationContext
        val dbName = databaseNameForQuizType(quizType)
        return Room.databaseBuilder(appContext, AppDatabase::class.java, dbName)
            .createFromAsset(dbName)
            .addMigrations(*AppDatabase.ALL_MIGRATIONS)
            .build()
    }

    /**
     * Returns the shared stats database (singleton, thread-safe).
     * This database stores quiz session results and gamification data.
     */
    fun getStatsDatabase(): StatsDatabase {
        return statsDatabase ?: synchronized(this) {
            statsDatabase ?: run {
                val appContext = requireNotNull(context) { "Context is required to create StatsDatabase" }.applicationContext
                Room.databaseBuilder(appContext, StatsDatabase::class.java, "quiz_stats.db")
                    .addMigrations(StatsDatabase.MIGRATION_1_2)
                    .build()
                    .also { statsDatabase = it }
            }
        }
    }
}
