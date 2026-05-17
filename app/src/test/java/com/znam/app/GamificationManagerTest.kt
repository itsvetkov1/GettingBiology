package com.znam.app

import com.znam.app.data.Achievement
import com.znam.app.data.Achievements
import com.znam.app.data.GamificationDao
import com.znam.app.data.UserProfile
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class GamificationManagerTest {
    private class FixedClock(private val date: LocalDate) : Clock {
        override fun today(): LocalDate = date
    }

    private class FakeGamificationDao(initialProfile: UserProfile? = null) : GamificationDao {
        private val achievements = linkedMapOf<String, Achievement>()
        private val profileFlow = MutableStateFlow(initialProfile)
        var profile: UserProfile?
            get() = profileFlow.value
            set(value) { profileFlow.value = value }

        override suspend fun getProfile(): UserProfile? = profile
        override fun observeProfile(): Flow<UserProfile?> = profileFlow
        override suspend fun insertProfile(profile: UserProfile) {
            if (this.profile == null) this.profile = profile
        }
        override suspend fun updateProfile(profile: UserProfile) { this.profile = profile }
        override suspend fun getAllAchievements(): List<Achievement> = achievements.values.toList()
        override fun observeAchievements(): Flow<List<Achievement>> = MutableStateFlow(achievements.values.toList())
        override suspend fun getUnlockedIds(): List<String> = achievements.keys.toList()
        override suspend fun unlockAchievement(achievement: Achievement) { achievements.putIfAbsent(achievement.achievementId, achievement) }
        override suspend fun isUnlocked(id: String): Int = if (id in achievements) 1 else 0
    }

    @Test
    fun firstQuiz_createsProfileAwardsXpStreakAndFirstQuizAchievement() = runBlocking {
        val dao = FakeGamificationDao()
        val result = GamificationManager(dao, clock = FixedClock(LocalDate.of(2026, 1, 10)))
            .processQuizCompletion(score = 8, totalQuestions = 15, elapsedTimeSeconds = 120, hintsUsed = 1)

        assertEquals(85, result.xpEarned)
        assertEquals(85, dao.profile!!.totalXp)
        assertEquals(1, dao.profile!!.currentStreak)
        assertTrue(result.newAchievements.contains(Achievements.FIRST_QUIZ))
    }

    @Test
    fun sameDaySecondQuizKeepsStreakAndIncrementsDailyCount() = runBlocking {
        val today = LocalDate.of(2026, 1, 10)
        val dao = FakeGamificationDao(UserProfile(currentStreak = 3, longestStreak = 3, lastQuizDateEpochDay = today.toEpochDay(), quizzesCompletedToday = 1))

        GamificationManager(dao, clock = FixedClock(today))
            .processQuizCompletion(score = 1, totalQuestions = 15, elapsedTimeSeconds = 120, hintsUsed = 1)

        assertEquals(3, dao.profile!!.currentStreak)
        assertEquals(2, dao.profile!!.quizzesCompletedToday)
    }

    @Test
    fun yesterdayQuizExtendsStreak() = runBlocking {
        val today = LocalDate.of(2026, 1, 10)
        val dao = FakeGamificationDao(UserProfile(currentStreak = 3, longestStreak = 3, lastQuizDateEpochDay = today.minusDays(1).toEpochDay()))

        GamificationManager(dao, clock = FixedClock(today))
            .processQuizCompletion(score = 1, totalQuestions = 15, elapsedTimeSeconds = 120, hintsUsed = 1)

        assertEquals(4, dao.profile!!.currentStreak)
        assertEquals(4, dao.profile!!.longestStreak)
    }

    @Test
    fun missedDayResetsStreakToOne() = runBlocking {
        val today = LocalDate.of(2026, 1, 10)
        val dao = FakeGamificationDao(UserProfile(currentStreak = 9, longestStreak = 9, lastQuizDateEpochDay = today.minusDays(3).toEpochDay()))

        GamificationManager(dao, clock = FixedClock(today))
            .processQuizCompletion(score = 1, totalQuestions = 15, elapsedTimeSeconds = 120, hintsUsed = 1)

        assertEquals(1, dao.profile!!.currentStreak)
        assertEquals(9, dao.profile!!.longestStreak)
    }

    @Test
    fun zeroQuestionQuizGetsOnlyStreakBonusAndNoPerfectSpeedBonuses() = runBlocking {
        val dao = FakeGamificationDao()
        val result = GamificationManager(dao, clock = FixedClock(LocalDate.of(2026, 1, 10)))
            .processQuizCompletion(score = 0, totalQuestions = 0, elapsedTimeSeconds = 1, hintsUsed = 0)

        assertEquals(5, result.xpEarned)
        assertEquals(listOf("Correct answers", "Streak bonus"), result.xpBreakdown.map { it.label })
    }

    @Test
    fun dailyChallengeBonusIsAwardedOncePerDay() = runBlocking {
        val today = LocalDate.of(2026, 1, 1)
        val dao = FakeGamificationDao()
        val daily = DailyChallengeManager(dao, FixedClock(today))
        val manager = GamificationManager(dao, daily, FixedClock(today))
        val quizType = daily.getTodaysChallengeType()

        val first = manager.processQuizCompletion(1, 15, 120, 1, isDailyChallenge = true, quizType = quizType)
        val second = manager.processQuizCompletion(1, 15, 120, 1, isDailyChallenge = true, quizType = quizType)

        assertTrue(first.xpBreakdown.any { it.label == "Daily challenge" })
        assertTrue(second.xpBreakdown.none { it.label == "Daily challenge" })
    }

    @Test
    fun crossingAchievementThresholdUnlocksNewAchievement() = runBlocking {
        val dao = FakeGamificationDao(UserProfile(totalQuizzesCompleted = 9, currentStreak = 1, longestStreak = 1))

        val result = GamificationManager(dao, clock = FixedClock(LocalDate.of(2026, 1, 10)))
            .processQuizCompletion(score = 1, totalQuestions = 15, elapsedTimeSeconds = 120, hintsUsed = 1)

        assertTrue(result.newAchievements.contains(Achievements.TEN_QUIZZES))
    }

    @Test
    fun concurrentCompletionsAreSerializedByMutex() = runBlocking {
        val dao = FakeGamificationDao()
        val manager = GamificationManager(dao, clock = FixedClock(LocalDate.of(2026, 1, 10)))

        (1..10).map {
            async { manager.processQuizCompletion(score = 1, totalQuestions = 15, elapsedTimeSeconds = 120, hintsUsed = 1) }
        }.awaitAll()

        assertEquals(10, dao.profile!!.totalQuizzesCompleted)
        assertEquals(10, dao.profile!!.quizzesCompletedToday)
    }
}
