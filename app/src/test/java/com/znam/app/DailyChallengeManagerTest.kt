package com.znam.app

import com.znam.app.data.Achievement
import com.znam.app.data.GamificationDao
import com.znam.app.data.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DailyChallengeManagerTest {
    private class MutableClock(var date: LocalDate) : Clock {
        override fun today(): LocalDate = date
    }

    private class FakeGamificationDao(initialProfile: UserProfile? = null) : GamificationDao {
        private val profileFlow = MutableStateFlow(initialProfile)
        var profile: UserProfile?
            get() = profileFlow.value
            set(value) { profileFlow.value = value }

        override suspend fun getProfile(): UserProfile? = profile
        override fun observeProfile(): Flow<UserProfile?> = profileFlow
        override suspend fun insertProfile(profile: UserProfile) { if (this.profile == null) this.profile = profile }
        override suspend fun updateProfile(profile: UserProfile) { this.profile = profile }
        override suspend fun getAllAchievements(): List<Achievement> = emptyList()
        override fun observeAchievements(): Flow<List<Achievement>> = MutableStateFlow(emptyList())
        override suspend fun getUnlockedIds(): List<String> = emptyList()
        override suspend fun unlockAchievement(achievement: Achievement) = Unit
        override suspend fun isUnlocked(id: String): Int = 0
    }

    @Test
    fun janFirstMapsToFirstChallengeType() {
        val manager = DailyChallengeManager(FakeGamificationDao(), MutableClock(LocalDate.of(2026, 1, 1)))

        assertEquals("class9.db", manager.getTodaysChallengeType())
    }

    @Test
    fun decThirtyFirstUsesDeterministicDayOfYearRotation() {
        val manager = DailyChallengeManager(FakeGamificationDao(), MutableClock(LocalDate.of(2026, 12, 31)))

        assertEquals("class10.db", manager.getTodaysChallengeType())
    }

    @Test
    fun leapDayParticipatesInRotationWithoutSkippingFirstIndex() {
        val manager = DailyChallengeManager(FakeGamificationDao(), MutableClock(LocalDate.of(2024, 2, 29)))

        assertEquals("db_entrance_exam.db", manager.getTodaysChallengeType())
    }

    @Test
    fun midnightRolloverUsesCurrentClockForCompletionState() = runBlocking {
        val clock = MutableClock(LocalDate.of(2026, 1, 1))
        val dao = FakeGamificationDao(UserProfile())
        val manager = DailyChallengeManager(dao, clock)
        val janFirstType = manager.getTodaysChallengeType()
        dao.profile = manager.markDailyChallengeCompleted(dao.profile!!, janFirstType)

        assertTrue(manager.isTodaysChallengeCompleted())

        clock.date = LocalDate.of(2026, 1, 2)

        assertFalse(manager.isTodaysChallengeCompleted())
        assertTrue(manager.shouldAwardDailyChallenge(true, manager.getTodaysChallengeType()))
    }

    @Test
    fun completedChallengeDoesNotAwardAgain() = runBlocking {
        val clock = MutableClock(LocalDate.of(2026, 1, 1))
        val dao = FakeGamificationDao(UserProfile())
        val manager = DailyChallengeManager(dao, clock)
        val quizType = manager.getTodaysChallengeType()
        dao.profile = manager.markDailyChallengeCompleted(dao.profile!!, quizType)

        assertTrue(manager.isTodaysChallengeCompleted())
        assertFalse(manager.shouldAwardDailyChallenge(true, quizType))
    }
}
