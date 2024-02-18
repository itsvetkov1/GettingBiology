package com.znam.app

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserProgressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertProgress(progress: UserProgress)

    @Query("SELECT * FROM user_progress ORDER BY questionId DESC LIMIT 1")
    fun getLastProgress(): UserProgress?
}
