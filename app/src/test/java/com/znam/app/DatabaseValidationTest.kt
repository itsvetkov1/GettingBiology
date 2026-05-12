package com.znam.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DatabaseValidationTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun validateAllDatabases() {
        val assets = context.assets.list("") ?: emptyArray()
        val dbFiles = assets.filter { it.endsWith(".db") }
        
        val errors = mutableListOf<String>()

        dbFiles.forEach { dbFile: String ->
            validateDatabase(dbFile, errors)
        }

        if (errors.isNotEmpty()) {
            fail("Found validation errors in databases:\n${errors.joinToString("\n")}")
        }
    }

    private fun validateDatabase(dbName: String, errors: MutableList<String>) {
        println("Validating database: $dbName")
        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .createFromAsset(dbName)
            .allowMainThreadQueries()
            .addMigrations(*AppDatabase.ALL_MIGRATIONS)
            .build()

        try {
            val questions = db.questionDao().getAllQuestions()
            questions.forEach { question ->
                val options = question.getParsedOptions()
                val correctAnswer = question.correctAnswer.trim()

                // 1. Check for empty question text
                if (question.questionText.isBlank()) {
                    errors.add("[$dbName] Question ID ${question.id}: Question text is empty")
                }

                // 2. Check for empty options
                if (options.isEmpty()) {
                    errors.add("[$dbName] Question ID ${question.id}: No options found")
                }

                // 3. Check for duplicate options
                val distinctOptions = options.map { it.lowercase() }.distinct()
                if (options.size != distinctOptions.size) {
                    errors.add("[$dbName] Question ID ${question.id}: Duplicate options found")
                }

                // 4. Check if correct answer is in options
                val hasCorrectAnswer = options.any { it.equals(correctAnswer, ignoreCase = true) }
                if (!hasCorrectAnswer) {
                    errors.add("[$dbName] Question ID ${question.id}: Correct answer '$correctAnswer' not found in options $options")
                }
            }
        } catch (e: Exception) {
            errors.add("[$dbName] Failed to read database: ${e.message}")
        } finally {
            db.close()
        }
    }
}
