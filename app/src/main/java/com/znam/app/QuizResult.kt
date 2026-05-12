package com.znam.app

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

const val SKIPPED_ANSWER = "__SKIPPED_ANSWER__"

@Parcelize
data class QuizResult(
    val score: Int,
    val questions: ArrayList<Question>,
    val userAnswers: ArrayList<String>,
    val elapsedTimeInSeconds: Int
) : Parcelable
