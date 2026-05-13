package com.znam.app

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.znam.app.data.StatsDatabase
import java.util.concurrent.ConcurrentHashMap

class DatabaseProvider(private val context: Context?) {

    private val appDatabases = ConcurrentHashMap<String, AppDatabase>()

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
        val dbName = databaseNameForQuizType(quizType)
        return appDatabases.getOrPut(dbName) {
            buildDatabase(dbName, AppDatabase::class.java)
                .createFromAsset(dbName)
                .addMigrations(*AppDatabase.ALL_MIGRATIONS)
                .build()
        }
    }

    /**
     * Returns the shared stats database (singleton, thread-safe).
     * This database stores quiz sessions, gamification, and question performance data.
     */
    fun getStatsDatabase(): StatsDatabase {
        return statsDatabase ?: synchronized(this) {
            statsDatabase ?: buildDatabase("quiz_stats.db", StatsDatabase::class.java)
                .addMigrations(
                    StatsDatabase.MIGRATION_1_2,
                    StatsDatabase.MIGRATION_2_3,
                    StatsDatabase.MIGRATION_3_4
                )
                .build()
                .also { statsDatabase = it }
        }
    }

    private fun <T : RoomDatabase> buildDatabase(
        dbName: String,
        databaseClass: Class<T>
    ): RoomDatabase.Builder<T> {
        val appContext = requireNotNull(context) { "Context is required to create Room databases" }.applicationContext
        return Room.databaseBuilder(appContext, databaseClass, dbName)
    }
}
