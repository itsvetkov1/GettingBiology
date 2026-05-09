package com.znam.app

import android.content.Context
import androidx.room.Room

class DatabaseProvider(private val context: Context?) {
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
}
