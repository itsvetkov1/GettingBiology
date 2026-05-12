package com.znam.app

import android.util.Log
import com.znam.app.data.QuestionPerformance
import com.znam.app.data.QuestionPerformanceDao

/**
 * Selects questions using a blend of spaced repetition and adaptive difficulty.
 *
 * Selection strategy for a session of N questions:
 * 1. Due-for-review questions (spaced repetition) — up to 40% of session
 * 2. Weak questions (high difficulty score) — up to 30% of session
 * 3. Unseen questions (never answered) — up to 20% of session
 * 4. Random fill from remaining pool — rest
 *
 * This ensures the user revisits forgotten material, practices weak areas,
 * discovers new content, and still gets variety.
 */
class SmartQuestionSelector(
    private val performanceDao: QuestionPerformanceDao
) {
    companion object {
        private const val TAG = "SmartSelector"
        private const val DUE_REVIEW_RATIO = 0.40f
        private const val WEAK_RATIO = 0.30f
        private const val UNSEEN_RATIO = 0.20f
        // Remaining 10% = random
    }

    /**
     * Select [count] questions from [allQuestions] using smart ordering.
     * Falls back to shuffled selection if no performance data exists.
     */
    suspend fun selectQuestions(
        allQuestions: List<Question>,
        quizType: String,
        count: Int
    ): List<Question> {
        if (allQuestions.isEmpty()) return emptyList()
        val needed = minOf(count, allQuestions.size)

        val performanceMap = performanceDao.getAllForQuizType(quizType)
            .associateBy { it.questionId }

        // If no history, just shuffle (first-time user)
        if (performanceMap.isEmpty()) {
            Log.d(TAG, "No performance history — shuffling $needed questions")
            return allQuestions.shuffled().take(needed)
        }

        val now = System.currentTimeMillis()
        val questionMap = allQuestions.associateBy { it.id }
        val selected = mutableListOf<Question>()
        val usedIds = mutableSetOf<Int>()

        // 1. Due for review
        val dueCount = (needed * DUE_REVIEW_RATIO).toInt().coerceAtLeast(1)
        val duePerformances = performanceDao.getDueForReview(quizType, now)
        for (perf in duePerformances) {
            if (selected.size >= dueCount) break
            val q = questionMap[perf.questionId]
            if (q != null && q.id !in usedIds) {
                selected.add(q)
                usedIds.add(q.id)
            }
        }
        Log.d(TAG, "Due-for-review: added ${selected.size} questions")

        // 2. Weak questions (high difficulty)
        val weakCount = (needed * WEAK_RATIO).toInt().coerceAtLeast(1)
        val weakPerformances = performanceDao.getWeakestQuestions(quizType, weakCount * 2)
        var weakAdded = 0
        for (perf in weakPerformances) {
            if (weakAdded >= weakCount) break
            val q = questionMap[perf.questionId]
            if (q != null && q.id !in usedIds) {
                selected.add(q)
                usedIds.add(q.id)
                weakAdded++
            }
        }
        Log.d(TAG, "Weak questions: added $weakAdded")

        // 3. Unseen questions
        val unseenCount = (needed * UNSEEN_RATIO).toInt().coerceAtLeast(1)
        val unseenQuestions = allQuestions.filter { it.id !in performanceMap && it.id !in usedIds }.shuffled()
        var unseenAdded = 0
        for (q in unseenQuestions) {
            if (unseenAdded >= unseenCount) break
            selected.add(q)
            usedIds.add(q.id)
            unseenAdded++
        }
        Log.d(TAG, "Unseen questions: added $unseenAdded")

        // 4. Random fill for the rest
        val remaining = needed - selected.size
        if (remaining > 0) {
            val pool = allQuestions.filter { it.id !in usedIds }.shuffled()
            for (q in pool) {
                if (selected.size >= needed) break
                selected.add(q)
                usedIds.add(q.id)
            }
            Log.d(TAG, "Random fill: added ${needed - selected.size + remaining} -> total ${selected.size}")
        }

        // Shuffle final selection so the user doesn't always get "hard first"
        return selected.shuffled()
    }

    /**
     * Record the result of answering a question. Updates performance tracking.
     */
    suspend fun recordAnswer(
        quizType: String,
        questionId: Int,
        wasCorrect: Boolean
    ) {
        val now = System.currentTimeMillis()
        val existing = performanceDao.getPerformance(quizType, questionId)

        val updated = if (existing != null) {
            val newConsecutiveCorrect = if (wasCorrect) existing.consecutiveCorrect + 1 else 0
            val newConsecutiveWrong = if (!wasCorrect) existing.consecutiveWrong + 1 else 0

            // Update difficulty score: exponential moving average
            // If wrong, difficulty increases; if right, it decreases
            val alpha = 0.3f  // smoothing factor
            val newDifficulty = if (wasCorrect) {
                existing.difficultyScore * (1 - alpha) + 0f * alpha
            } else {
                existing.difficultyScore * (1 - alpha) + 1f * alpha
            }

            existing.copy(
                timesAnswered = existing.timesAnswered + 1,
                timesCorrect = existing.timesCorrect + (if (wasCorrect) 1 else 0),
                consecutiveCorrect = newConsecutiveCorrect,
                consecutiveWrong = newConsecutiveWrong,
                lastAnsweredAt = now,
                nextReviewAt = now + existing.computeNextInterval(wasCorrect),
                difficultyScore = newDifficulty.coerceIn(0f, 1f)
            )
        } else {
            // First time seeing this question
            val perf = QuestionPerformance(
                quizType = quizType,
                questionId = questionId,
                timesAnswered = 1,
                timesCorrect = if (wasCorrect) 1 else 0,
                consecutiveCorrect = if (wasCorrect) 1 else 0,
                consecutiveWrong = if (!wasCorrect) 1 else 0,
                lastAnsweredAt = now,
                difficultyScore = if (wasCorrect) 0.3f else 0.7f
            )
            perf.copy(nextReviewAt = now + perf.computeNextInterval(wasCorrect))
        }

        performanceDao.upsert(updated)
    }
}
