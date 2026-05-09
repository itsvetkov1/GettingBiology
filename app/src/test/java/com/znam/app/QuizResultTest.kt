package com.znam.app

import android.os.Parcel
import android.os.Parcelable
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class QuizResultTest {
    @Test
    fun parcelableRoundTrip_preservesQuizResultData() {
        val result = QuizResult(
            score = 1,
            questions = arrayListOf(Question(7, "Q", "A;B", "A", "hint 1", "hint 2")),
            userAnswers = arrayListOf("A"),
            elapsedTimeInSeconds = 42
        )

        val parcel = Parcel.obtain()
        result.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        @Suppress("UNCHECKED_CAST")
        val creator = QuizResult::class.java.getField("CREATOR").get(null) as Parcelable.Creator<QuizResult>
        val restored = creator.createFromParcel(parcel)
        parcel.recycle()

        assertEquals(result.score, restored.score)
        assertEquals(result.questions, restored.questions)
        assertEquals(result.userAnswers, restored.userAnswers)
        assertEquals(result.elapsedTimeInSeconds, restored.elapsedTimeInSeconds)
    }
}
