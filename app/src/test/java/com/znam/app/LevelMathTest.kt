package com.znam.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelMathTest {
    @Test
    fun levelFromXp_matchesBoundaryExpectations() {
        assertEquals(1, GamificationManager.levelFromXp(0))
        assertEquals(1, GamificationManager.levelFromXp(99))
        assertEquals(2, GamificationManager.levelFromXp(100))
        assertEquals(2, GamificationManager.levelFromXp(299))
        assertEquals(3, GamificationManager.levelFromXp(300))
        assertEquals(4, GamificationManager.levelFromXp(999))
        assertEquals(5, GamificationManager.levelFromXp(1000))
    }

    @Test
    fun levelProgress_resetsAtBoundariesAndStaysWithinCurrentLevel() {
        assertEquals(0f, GamificationManager.levelProgress(0), 0.0001f)
        assertEquals(0.99f, GamificationManager.levelProgress(99), 0.0001f)
        assertEquals(0f, GamificationManager.levelProgress(100), 0.0001f)
        assertEquals(0.995f, GamificationManager.levelProgress(299), 0.0001f)
        assertEquals(0f, GamificationManager.levelProgress(300), 0.0001f)
        assertTrue(GamificationManager.levelProgress(999) < 1f)
        assertEquals(0f, GamificationManager.levelProgress(1000), 0.0001f)
    }
}
