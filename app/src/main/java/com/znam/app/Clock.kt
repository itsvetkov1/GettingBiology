package com.znam.app

import java.time.LocalDate

/**
 * Injectable source of date/time for day-sensitive logic.
 */
interface Clock {
    fun today(): LocalDate
    fun currentTimeMillis(): Long = System.currentTimeMillis()
}

object SystemClock : Clock {
    override fun today(): LocalDate = LocalDate.now()
}
