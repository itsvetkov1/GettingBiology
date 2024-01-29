package com.example.gettingbiology

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_progress")
data class UserProgress(
    @PrimaryKey val questionId: Int,
    val isCompleted: Boolean
)
