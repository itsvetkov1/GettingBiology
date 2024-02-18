package com.znam.app

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface QuestionDao {
    @Query("SELECT * FROM questions")
    fun getAllQuestions(): List<Question>

    @Insert
    fun insertAll(vararg questions: Question)
}
